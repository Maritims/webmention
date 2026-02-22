package no.clueless.webmention.api_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.clueless.oauth.TokenResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.Optional;

public class AuthenticatingHttpClient {
    private static final Logger        log        = LoggerFactory.getLogger(AuthenticatingHttpClient.class);
    private static final ObjectMapper  jsonMapper = new JsonMapper().registerModule(new JavaTimeModule());
    @NotNull
    private final        HttpClient    httpClient;
    @Nullable
    private final        URI           tokenUri;
    @Nullable
    private final        String        clientId;
    @Nullable
    private final        String        clientSecret;
    @Nullable
    private              TokenResponse tokenResponse;
    @Nullable
    private              ZonedDateTime lastTokenFetch;

    public AuthenticatingHttpClient(@NotNull HttpClient httpClient, @NotNull URI tokenUri, @NotNull String clientId, @NotNull String clientSecret) {
        this.httpClient   = httpClient;
        this.tokenUri     = tokenUri;
        this.clientId     = clientId;
        this.clientSecret = clientSecret;
    }

    public AuthenticatingHttpClient(@NotNull HttpClient httpClient) {
        this.httpClient   = httpClient;
        this.tokenUri     = null;
        this.clientId     = null;
        this.clientSecret = null;
    }

    @NotNull
    String ensureValidAccessToken() {
        if (tokenResponse == null || lastTokenFetch == null || lastTokenFetch.plusSeconds(tokenResponse.expiresIn()).isBefore(ZonedDateTime.now())) {
            var tokenUri     = Optional.ofNullable(this.tokenUri).orElseGet(() -> Optional.ofNullable(System.getenv("WEBMENTION_TOKEN_URI")).filter(value -> !value.isBlank()).map(URI::create).orElseThrow(() -> new IllegalStateException("WEBMENTION_TOKEN_URI must be set")));
            var clientId     = Optional.ofNullable(this.clientId).orElseGet(() -> Optional.ofNullable(System.getenv("WEBMENTION_CLIENT_ID")).filter(value -> !value.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_CLIENT_ID must be set")));
            var clientSecret = Optional.ofNullable(this.clientSecret).orElseGet(() -> Optional.ofNullable(System.getenv("WEBMENTION_CLIENT_SECRET")).filter(value -> !value.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_CLIENT_SECRET must be set")));
            var httpRequest = HttpRequest.newBuilder()
                    .uri(tokenUri)
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret))
                    .build();

            HttpResponse<String> httpResponse;
            try {
                httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("HTTP request to oauth2/token failed", e);
            }

            if (httpResponse.statusCode() != 200) {
                log.error("Failed to fetch access token. Status code was {} and response body was:\n{}", httpResponse.statusCode(), httpResponse.body());
            }

            try {
                tokenResponse  = jsonMapper.readValue(httpResponse.body(), TokenResponse.class);
                lastTokenFetch = ZonedDateTime.now();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse access token response", e);
            }
        }

        if (tokenResponse == null) {
            throw new IllegalStateException("Failed to fetch access token");
        }
        return tokenResponse.accessToken();
    }

    @NotNull
    public HttpResponse<Void> delete(@NotNull URI uri) throws IOException, InterruptedException {
        var accessToken = ensureValidAccessToken();
        var httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    @NotNull
    public HttpResponse<String> get(@NotNull URI uri) throws IOException, InterruptedException {
        var accessToken = ensureValidAccessToken();
        var httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    @NotNull
    public HttpResponse<String> patch(@NotNull URI uri, @Nullable String body) throws IOException, InterruptedException {
        var accessToken = ensureValidAccessToken();
        var httpRequest = HttpRequest.newBuilder(uri)
                .method("PATCH", body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + accessToken)
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    @NotNull
    public HttpResponse<String> patch(@NotNull URI uri) throws IOException, InterruptedException {
        return patch(uri, null);
    }

    @NotNull
    public HttpResponse<String> post(@NotNull URI uri, @Nullable String body) throws IOException, InterruptedException {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body cannot be null or blank");
        }

        var accessToken = ensureValidAccessToken();
        var httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + accessToken)
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }
}
