package no.clueless.webmention.api_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.clueless.interceptable_http_client.InterceptableHttpClient;
import no.clueless.interceptable_http_client.OAuth2ClientCredentialsHttpRequestInterceptor;
import no.clueless.webmention.persistence.Webmention;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class WebmentionManagementApiClient {
    private static final ObjectMapper jsonMapper = new JsonMapper().registerModule(new JavaTimeModule());
    @NotNull
    private final        URI          webmentionsManagementApiUri;
    @NotNull
    private final        HttpClient   httpClient;

    public WebmentionManagementApiClient(@NotNull URI webmentionsManagementApiUri) {
        this.webmentionsManagementApiUri = webmentionsManagementApiUri;
        this.httpClient = new InterceptableHttpClient(HttpClient.newBuilder().build())
                .addInterceptor(new OAuth2ClientCredentialsHttpRequestInterceptor(
                        1,
                        () -> new OAuth2ClientCredentialsHttpRequestInterceptor.Credentials("foo", "bar"),
                        "http://localhost:7070/oauth/token",
                        JsonMapper::new
                ));
    }

    @NotNull
    public List<Webmention> getWebmentions(@NotNull Pagination pagination, @Nullable Boolean isApproved) {
        String responseBody;

        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(webmentionsManagementApiUri.resolve("?" + pagination.toQueryString() + (isApproved == null ? "" : "&isApproved=" + isApproved)))
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
            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch webmentions: " + httpResponse.body());
            }

            var contentType = httpResponse.headers().firstValue("content-type").orElse(null);
            if (!"application/json".equalsIgnoreCase(contentType)) {
                throw new RuntimeException("Invalid content-type: " + contentType);
            }

            responseBody = httpResponse.body();
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

    public void publishWebmention(int webmentionId) {
        if (webmentionId < 1) {
            throw new IllegalArgumentException("webmentionId must be greater than 0");
        }

        HttpResponse<String> httpResponse;
        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(webmentionsManagementApiUri.resolve("publish/" + webmentionId))
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
                    .uri(webmentionsManagementApiUri.resolve("unpublish/" + webmentionId))
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
                    .uri(webmentionsManagementApiUri.resolve("delete/" + webmentionId))
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
