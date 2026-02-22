package no.clueless.webmention.api_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.clueless.webmention.persistence.Webmention;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class WebmentionApiClient {
    private static final ObjectMapper jsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @NotNull
    private final        URI          baseUri;
    @NotNull
    private final        HttpClient   httpClient;

    public WebmentionApiClient(@NotNull URI baseUri, @NotNull HttpClient httpClient) {
        this.baseUri    = baseUri;
        this.httpClient = httpClient;
    }

    public List<Webmention> getWebmentions(@NotNull Pagination pagination) {
        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(baseUri.resolve("webmention?" + pagination.toQueryString()))
                    .GET()
                    .build();
            var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 204) {
                return List.of();
            }
            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed to get webmentions: " + httpResponse.body());
            }
            return jsonMapper.readValue(httpResponse.body(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse webmentions response", e);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to get webmentions", e);
        }
    }

    @NotNull
    public List<Webmention> getWebmentions() {
        return getWebmentions(new Pagination());
    }

    public void postWebmention(@NotNull Webmention webmention) {
        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(baseUri.resolve("webmention"))
                    .GET()
                    .build();
            var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if(httpResponse.statusCode() == 200 || httpResponse.statusCode() == 201 || httpResponse.statusCode() == 202) {
                return;
            }
            throw new RuntimeException("Failed to post webmention: " + httpResponse.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize webmention", e);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to post webmention", e);
        }
    }
}
