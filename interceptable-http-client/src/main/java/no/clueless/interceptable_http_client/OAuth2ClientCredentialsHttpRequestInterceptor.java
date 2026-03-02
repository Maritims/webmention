package no.clueless.interceptable_http_client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Supplier;

public class OAuth2ClientCredentialsHttpRequestInterceptor implements HttpRequestInterceptor {
    private final int                   priority;
    @NotNull
    private final Supplier<Credentials> credentialsProvider;
    @NotNull
    private final String                tokenUrl;
    @NotNull
    private final Supplier<JsonMapper>  jsonMapperProvider;

    @Nullable
    private ZonedDateTime lastTokenFetch;
    @Nullable
    private TokenResponse tokenResponse;

    public OAuth2ClientCredentialsHttpRequestInterceptor(
            int priority,
            @NotNull Supplier<Credentials> credentialsProvider,
            @NotNull String tokenUrl,
            @NotNull Supplier<JsonMapper> jsonMapperProvider
    ) {
        if (priority < 0) {
            throw new IllegalArgumentException("priority must be >= 0");
        }
        this.priority            = priority;
        this.credentialsProvider = credentialsProvider;
        this.tokenUrl            = tokenUrl;
        this.jsonMapperProvider  = jsonMapperProvider;
    }

    @Override
    public HttpRequest intercept(@NotNull HttpRequest originalRequest, @NotNull HttpClient httpClient) {
        if (tokenResponse == null || lastTokenFetch == null || ZonedDateTime.now().isAfter(lastTokenFetch.plusSeconds(tokenResponse.expiresIn - 10))) {
            var credentials = credentialsProvider.get();
            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&client_id=" + credentials.clientId + "&client_secret=" + credentials.clientSecret))
                    .build();

            HttpResponse<String> httpResponse;
            try {
                httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (ConnectException e) {
                throw new RuntimeException("Failed to connect to " + tokenUrl, e);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("HTTP request to " + tokenUrl + " failed", e);
            }

            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch access token: " + httpResponse.body());
            }

            try {
                tokenResponse  = jsonMapperProvider.get().readValue(httpResponse.body(), TokenResponse.class);
                lastTokenFetch = ZonedDateTime.now();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse access token response", e);
            }
        }

        if (tokenResponse == null) {
            throw new IllegalStateException("Failed to fetch access token");
        }

        return HttpRequest.newBuilder(originalRequest, (name, value) -> true)
                .header("Authorization", "Bearer " + tokenResponse.accessToken)
                .build();
    }

    public record Credentials(@NotNull String clientId, @NotNull String clientSecret) {
        @NotNull
        public static Credentials fromEnvironment(@NotNull String clientIdKey, @NotNull String clientSecretKey) {
            var clientId     = Optional.ofNullable(System.getenv(clientIdKey)).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Missing environment variable: " + clientIdKey));
            var clientSecret = Optional.ofNullable(System.getenv(clientSecretKey)).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalArgumentException("Missing environment variable: " + clientSecretKey));
            return new Credentials(clientId, clientSecret);
        }
    }

    private record TokenResponse(
            @NotNull @JsonProperty("access_token") String accessToken,
            @NotNull @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            @Nullable @JsonProperty("scope") String scope
    ) {
    }
}
