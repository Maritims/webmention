package no.clueless.webmention.api_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.clueless.interceptable_http_client.InterceptableHttpClient;
import no.clueless.interceptable_http_client.OAuth2ClientCredentialsHttpRequestInterceptor;
import no.clueless.interceptable_http_client.Slf4jHttpRequestInterceptor;
import no.clueless.interceptable_http_client.Slf4jHttpResponseInterceptor;
import no.clueless.webmention.core.persistence.Webmention;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class WebmentionManagementApiClient {
    private static final Logger       log        = LoggerFactory.getLogger(WebmentionManagementApiClient.class);
    private static final ObjectMapper jsonMapper = new JsonMapper().registerModule(new JavaTimeModule());
    @NotNull
    private final        URI          baseUri;
    @NotNull
    private final        String       managementEndpoint;
    @NotNull
    private final        HttpClient   httpClient;

    public WebmentionManagementApiClient(@NotNull URI baseUri, @NotNull String tokenEndpoint, @NotNull String managementEndpoint) {
        if (tokenEndpoint.isBlank()) {
            throw new IllegalArgumentException("tokenEndpoint cannot be blank");
        }
        if (managementEndpoint.isBlank()) {
            throw new IllegalArgumentException("managementEndpoint cannot be blank");
        }

        if (log.isDebugEnabled()) {
            log.debug("Initializing WebmentionManagementApiClient with baseUri: {}, tokenEndpoint: {}, managementEndpoint: {}", baseUri, tokenEndpoint, managementEndpoint);
        }

        this.baseUri            = baseUri;
        this.managementEndpoint = managementEndpoint;
        this.httpClient         = new InterceptableHttpClient(HttpClient.newBuilder().build())
                .addInterceptor(new Slf4jHttpRequestInterceptor())
                .addInterceptor(new OAuth2ClientCredentialsHttpRequestInterceptor(
                        1,
                        () -> new OAuth2ClientCredentialsHttpRequestInterceptor.Credentials("foo", "bar"),
                        baseUri.resolve(tokenEndpoint).toString(),
                        JsonMapper::new
                ))
                .addInterceptor(new Slf4jHttpResponseInterceptor());

    }

    @NotNull
    public List<Webmention> getWebmentions(@NotNull Pagination pagination, @Nullable Boolean isApproved) {
        String responseBody;

        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUri.resolve(managementEndpoint) + "?" + pagination.toQueryString() + (isApproved == null ? "" : "&isApproved=" + isApproved)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 204) {
                return List.of();
            }
            if (httpResponse.statusCode() == 400) {
                throw new RuntimeException("An invalid request was made: " + httpResponse.body());
            }
            if (httpResponse.statusCode() == 404) {
                throw new RuntimeException("The requested resource " + httpRequest.uri() + " was not found: " + httpResponse.body());
            }
            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch webmentions: " + httpResponse.body());
            }

            var contentType = httpResponse.headers().firstValue("content-type").orElse(null);
            if (!"application/json".equalsIgnoreCase(contentType)) {
                throw new RuntimeException("Invalid content-type: " + contentType);
            }

            responseBody = httpResponse.body();

            if (log.isDebugEnabled()) {
                log.debug("Received webmentions response: {}", responseBody);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch webmentions", e);
        }

        try {
            return jsonMapper.readValue(responseBody, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse webmentions response: " + responseBody, e);
        }
    }

    @Nullable
    public Webmention getWebmention(int webmentionId) {
        if (webmentionId < 1) {
            throw new IllegalArgumentException("webmentionId must be greater than 0");
        }

        HttpResponse<String> httpResponse;
        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(baseUri.resolve("webmentions/" + webmentionId))
                    .GET()
                    .build();
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch webmention", e);
        }

        if (httpResponse.statusCode() == 200) {
            try {
                return jsonMapper.readValue(httpResponse.body(), Webmention.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse webmention response: " + httpResponse.body(), e);
            }
        } else {
            return null;
        }
    }

    public void publishWebmention(int webmentionId) {
        if (webmentionId < 1) {
            throw new IllegalArgumentException("webmentionId must be greater than 0");
        }

        HttpResponse<String> httpResponse;
        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(baseUri.resolve(managementEndpoint).resolve("publish/" + webmentionId))
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to publish webmention", e);
        }
        if (httpResponse.statusCode() == 204) {
            return;
        }
        throw new RuntimeException("Failed to publish webmention: " + httpResponse.body());
    }

    public void unpublishWebmention(int webmentionId) {
        if (webmentionId < 1) {
            throw new IllegalArgumentException("webmentionId must be greater than 0");
        }

        HttpResponse<String> httpResponse;
        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(baseUri.resolve(managementEndpoint).resolve("unpublish/" + webmentionId))
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to unpublish webmention", e);
        }
        if (httpResponse.statusCode() == 204) {
            return;
        }
        throw new RuntimeException("Failed to unpublish webmention: " + httpResponse.body());
    }

    public void deleteWebmention(int webmentionId) {
        if (webmentionId < 1) {
            throw new IllegalArgumentException("webmentionId must be greater than 0");
        }

        HttpResponse<Void> httpResponse;
        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(baseUri.resolve(managementEndpoint).resolve("delete/" + webmentionId))
                    .DELETE()
                    .build();
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to delete webmention", e);
        }
        if (httpResponse.statusCode() == 204) {
            return;
        }
        throw new RuntimeException("Failed to delete webmention: " + httpResponse.body());
    }
}
