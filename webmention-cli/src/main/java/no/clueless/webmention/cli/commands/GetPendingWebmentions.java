package no.clueless.webmention.cli.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.clueless.oauth.TokenResponse;
import no.clueless.webmention.cli.Command;
import no.clueless.webmention.cli.CommandResult;
import no.clueless.webmention.persistence.Webmention;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Gets pending webmentions from a webmention endpoint.
 */
public class GetPendingWebmentions implements Command {
    private static final Logger       log          = org.slf4j.LoggerFactory.getLogger(GetPendingWebmentions.class);
    private static final ObjectMapper objectMapper = new JsonMapper().registerModule(new JavaTimeModule());

    private final String clientId;
    private final String clientSecret;
    private final String oauthTokenEndpoint;
    private final String webmentionApiEndpoint;

    /**
     * Constructor.
     *
     * @param clientId              the client id
     * @param clientSecret          the client secret
     * @param oauthTokenEndpoint    the OAuth token endpoint
     * @param webmentionApiEndpoint the webmention API endpoint
     * @throws IllegalArgumentException if clientId, clientSecret, oauthTokenEndpoint, or webmentionApiEndpoint is null or blank.
     */
    public GetPendingWebmentions(String clientId, String clientSecret, String oauthTokenEndpoint, String webmentionApiEndpoint) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }
        if (oauthTokenEndpoint == null || oauthTokenEndpoint.isBlank()) {
            throw new IllegalArgumentException("oauthTokenEndpoint cannot be null or blank");
        }
        if (webmentionApiEndpoint == null || webmentionApiEndpoint.isBlank()) {
            throw new IllegalArgumentException("webmentionApiEndpoint cannot be null or blank");
        }
        this.clientId              = clientId;
        this.clientSecret          = clientSecret;
        this.oauthTokenEndpoint    = oauthTokenEndpoint;
        this.webmentionApiEndpoint = webmentionApiEndpoint;
    }

    /**
     * Default constructor. Reads client id, client secret, OAuth token endpoint and webmention API endpoint from environment variables.
     */
    public GetPendingWebmentions() {
        this.clientId              = System.getenv("WEBMENTION_CLIENT_ID");
        this.clientSecret          = System.getenv("WEBMENTION_CLIENT_SECRET");
        this.oauthTokenEndpoint    = System.getenv("WEBMENTION_OAUTH_TOKEN_ENDPOINT");
        this.webmentionApiEndpoint = System.getenv("WEBMENTION_API_ENDPOINT");
    }

    /**
     * Reads input from the command line until a non-empty value is entered, unless allowEmptyInput is true.
     *
     * @param label           the label to display in the prompt
     * @param reader          the scanner to read input from
     * @param allowEmptyInput whether to allow empty input
     * @return the entered value
     */
    private String readInput(String label, Scanner reader, boolean allowEmptyInput) {
        String input = null;
        while (input == null || input.isBlank()) {
            System.out.print("Enter " + label + ": ");
            input = reader.nextLine();
            if (allowEmptyInput || (input != null && !input.isBlank())) {
                return input;
            }
            System.err.println(label + " cannot be empty. Please try again.");
        }
        return input;
    }

    /**
     * Authenticates with the OAuth token endpoint and returns an access token.
     *
     * @param httpClient         the HTTP client to use
     * @param uri                the base URI of the webmention endpoint
     * @param clientId           the client id
     * @param clientSecret       the client secret
     * @param oauthTokenEndpoint the OAuth token endpoint
     * @return the access token
     */
    String getAccessToken(HttpClient httpClient, URI uri, String clientId, String clientSecret, String oauthTokenEndpoint) {
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient cannot be null");
        }

        var oauthTokenUri = uri.resolve(oauthTokenEndpoint);
        log.debug("Fetching access token from {}", oauthTokenUri);

        var objectMapper = new JsonMapper();
        var grantType    = "client_credentials";

        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(oauthTokenUri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(String.format("grant_type=%s&client_id=%s&client_secret=%s", grantType, clientId, clientSecret)))
                    .build();

            var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                log.error("Failed to fetch access token. Status code was {} and response body was:\n{}", httpResponse.statusCode(), httpResponse.body());
                return null;
            }

            var tokenResponse = objectMapper.readValue(httpResponse.body(), TokenResponse.class);
            log.debug("Received access token from {}", oauthTokenUri);
            return tokenResponse.accessToken();
        } catch (ConnectException e) {
            log.error("Connection to {} was refused. Please check that the URI is correct and that the webmention endpoint is running", oauthTokenUri);
            return null;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request to OAuth token endpoint failed", e);
        }
    }

    /**
     * Gets pending webmentions from the webmention API endpoint.
     *
     * @param httpClient  the HTTP client to use
     * @param uri         the base URI of the webmention endpoint
     * @param accessToken the access token to use
     * @return the pending webmentions
     * @throws IllegalArgumentException if httpClient, uri, accessToken, or webmentionApiEndpoint is null
     * @throws RuntimeException         if the HTTP request to the webmention API endpoint failed, or the response could not be parsed
     */
    List<Webmention> getPendingWebmentions(HttpClient httpClient, URI uri, String accessToken) {
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient cannot be null");
        }
        if (uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken cannot be null or blank");
        }

        try {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 204) {
                log.info("No pending webmentions found");
            } else if (httpResponse.statusCode() == 401) {
                log.error("Failed to fetch pending webmentions. Unauthorized");
            } else if (httpResponse.statusCode() == 404) {
                log.error("Failed to fetch pending webmentions from {}. The endpoint was not found", uri);
            } else if (httpResponse.statusCode() == 200) {
                return objectMapper.readValue(httpResponse.body(), new TypeReference<>() {
                });
            } else {
                log.error("Failed to fetch pending webmentions. Status code was {} and response body was:\n{}", httpResponse.statusCode(), httpResponse.body());
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse pending webmentions response", e);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request to webmention API failed", e);
        }

        return List.of();
    }

    public CommandResult execute(String[] args) {
        URI uri = null;

        for (var i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--uri", "-u" -> {
                    if (++i < args.length) {
                        uri = URI.create(args[i]);
                        log.debug("Setting uri to: {}", uri);
                    }
                }
                default -> {
                    return CommandResult.UNKNOWN_ARGUMENT;
                }
            }
        }

        if (uri == null) {
            return CommandResult.MISSING_REQUIRED_ARGUMENT;
        }

        var reader                = new Scanner(System.in);
        var clientId              = Optional.ofNullable(this.clientId).filter(value -> !value.isBlank()).orElseGet(() -> readInput("client id", reader, false));
        var clientSecret          = Optional.ofNullable(this.clientSecret).filter(value -> !value.isBlank()).orElseGet(() -> readInput("client secret", reader, false));
        var oauthTokenEndpoint    = Optional.ofNullable(this.oauthTokenEndpoint).filter(value -> !value.isBlank()).orElseGet(() -> Optional.ofNullable(readInput("OAuth token endpoint (leave empty for default: /oauth/token)", reader, true)).filter(value -> !value.isBlank()).orElse("/oauth/token"));
        var webmentionApiEndpoint = Optional.ofNullable(this.webmentionApiEndpoint).filter(value -> !value.isBlank()).orElseGet(() -> Optional.ofNullable(readInput("API endpoint (leave empty for default: /api/webmention)", reader, true)).filter(value -> !value.isBlank()).orElse("/api/webmention"));

        try (var httpClient = HttpClient.newBuilder().build()) {
            var accessToken = getAccessToken(httpClient, uri, clientId, clientSecret, oauthTokenEndpoint);
            if (accessToken == null) {
                return CommandResult.FAILURE;
            }

            var webmentions = getPendingWebmentions(httpClient, uri.resolve(webmentionApiEndpoint), accessToken);
            webmentions.forEach(webmention -> log.info("Pending webmention: {} -> {} (id: {})", webmention.sourceUrl(), webmention.targetUrl(), webmention.id()));
        }

        return CommandResult.SUCCESS;
    }
}
