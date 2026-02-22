package no.clueless.webmention.api_client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.clueless.webmention.persistence.Webmention;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

public class WebmentionManagementApiClient {
    private static final ObjectMapper jsonMapper = new JsonMapper().registerModule(new JavaTimeModule());
    @NotNull
    private final        URI          baseUri;
    @NotNull
    private final        HttpClient   httpClient;

    public WebmentionManagementApiClient(@NotNull URI baseUri, @NotNull HttpClient httpClient) {
        this.baseUri    = baseUri;
        this.httpClient = httpClient;
    }

    @NotNull
    public List<Webmention> getWebmentions(@NotNull Pagination pagination, @Nullable Boolean isApproved) {
        var httpClient = new AuthenticatingHttpClient(this.httpClient);
        try {
            var httpResponse = httpClient.get(baseUri.resolve("/api/webmentions/manage?" + pagination.toQueryString() + (isApproved == null ? "" : "&isApproved=" + isApproved)));
            if (httpResponse.statusCode() == 204) {
                return List.of();
            }
            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch webmentions: " + httpResponse.body());
            }
            return jsonMapper.readValue(httpResponse.body(), new TypeReference<>() {
            });
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch webmentions", e);
        }
    }

    public void publishWebmention(int webmentionId) {
        if (webmentionId < 1) {
            throw new IllegalArgumentException("webmentionId must be greater than 0");
        }

        var                  httpClient   = new AuthenticatingHttpClient(this.httpClient);
        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.patch(baseUri.resolve("/api/webmentions/publish/" + webmentionId));
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

        var                  httpClient   = new AuthenticatingHttpClient(this.httpClient);
        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.patch(baseUri.resolve("/api/webmentions/unpublish/" + webmentionId));
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

        var httpClient = new AuthenticatingHttpClient(this.httpClient);
        HttpResponse<Void> httpResponse;
        try {
            httpResponse = httpClient.delete(baseUri.resolve("/api/webmentions/delete/" + webmentionId));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to delete webmention", e);
        }
        if (httpResponse.statusCode() == 204) {
            return;
        }
        throw new RuntimeException("Failed to delete webmention: " + httpResponse.body());
    }
}
