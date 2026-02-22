package no.clueless.webmention.service;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Webmention service options.
 *
 * @param httpServerPort                  the port to run the webmention service on
 * @param databaseConnectionString        the JDBC connection string to the database
 * @param webmentionEndpoint              the endpoint to send webmentions to
 * @param supportedDomains                the domains to accept webmentions on behalf of
 * @param connectTimeout                  the connect timeout for connections to webmention senders when verifying webmentions
 * @param oauthIssuer                     the OAuth 2.0 access token issuer
 * @param oauthJwtSecret                  the OAuth 2.0 access token signing secret
 * @param oauthAccessTokenValiditySeconds the OAuth 2.0 access token validity in seconds
 * @param isOauthManagementApiEnabled     whether to enable the OAuth 2.0 management API
 * @param testMode                        whether to run in test mode
 * @param testPages                       the test pages to use
 */
public record Options(
        int httpServerPort,
        @NotNull String databaseConnectionString,
        @NotNull String webmentionEndpoint,
        @NotNull Set<String> supportedDomains,
        long connectTimeout,
        @NotNull String oauthIssuer,
        @NotNull String oauthJwtSecret,
        long oauthAccessTokenValiditySeconds,
        boolean isOauthManagementApiEnabled,
        boolean testMode,
        @NotNull Set<String> testPages
) {
    /**
     * Constructor.
     *
     * @param httpServerPort                  the port to run the webmention service on. Must be between 1024 and 65535.
     * @param databaseConnectionString        the JDBC connection string to the database. Must not be null or blank.
     * @param webmentionEndpoint              the endpoint to send webmentions to. Must not be null or blank.
     * @param supportedDomains                the domains to accept webmentions on behalf of. Must not be null or empty.
     * @param connectTimeout                  the connect timeout for connections to webmention senders when verifying webmentions. Must be greater than 0.
     * @param oauthIssuer                     the OAuth 2.0 access token issuer. Must not be null or blank.
     * @param oauthJwtSecret                  the OAuth 2.0 access token signing secret. Must not be null or blank.
     * @param oauthAccessTokenValiditySeconds the OAuth 2.0 access token validity in seconds. Must be greater than 0.
     * @param isOauthManagementApiEnabled     whether to enable the OAuth 2.0 management API.
     * @param testMode                        whether to run in test mode.
     * @param testPages                       the test pages to use.
     * @throws IllegalArgumentException if any of the parameters are invalid.
     */
    public Options {
        if (httpServerPort < 1024 || httpServerPort > 65535) {
            throw new IllegalArgumentException("httpServerPort must be between 1024 and 65535");
        }
        if (databaseConnectionString.isBlank()) {
            throw new IllegalArgumentException("databaseConnectionString cannot be blank");
        }
        if (webmentionEndpoint.isBlank()) {
            throw new IllegalArgumentException("webmentionEndpoint cannot be blank");
        }
        if (supportedDomains.isEmpty()) {
            throw new IllegalArgumentException("supportedDomains cannot be empty");
        }
        if (oauthIssuer.isBlank()) {
            throw new IllegalArgumentException("oauthIssuer cannot be blank");
        }
        if (oauthJwtSecret.isBlank()) {
            throw new IllegalArgumentException("oauthJwtSecret cannot be blank");
        }
        if (oauthAccessTokenValiditySeconds <= 0) {
            throw new IllegalArgumentException("oauthAccessTokenValiditySeconds must be greater than 0");
        }
    }

    /**
     * Loads the options from environment variables.
     *
     * @return the options
     * @throws IllegalStateException if any required environment variables are missing:
     *                               <ul>
     *                                   <li>WEBMENTION_SUPPORTED_DOMAINS</li>
     *                                   <li>WEBMENTION_ISSUER</li>
     *                                   <li>WEBMENTION_JWT_SECRET</li>
     *                               </ul>
     */
    @NotNull
    public static Options fromEnvironment() {
        return new Options(
                Optional.ofNullable(System.getenv("WEBMENTION_SERVER_PORT")).filter(property -> !property.isBlank()).map(Integer::parseInt).orElse(8080),
                Optional.ofNullable(System.getenv("WEBMENTION_DB_CONNECTION_STRING")).filter(property -> !property.isBlank()).orElse("jdbc:sqlite:webmentions.db"),
                Optional.ofNullable(System.getenv("WEBMENTION_ENDPOINT")).orElse("/webmention"),
                Optional.ofNullable(System.getenv("WEBMENTION_SUPPORTED_DOMAINS")).map(value -> new HashSet<>(Arrays.asList(value.split(",")))).orElseThrow(() -> new IllegalStateException("WEBMENTION_SUPPORTED_DOMAINS must be set")),
                Optional.ofNullable(System.getenv("WEBMENTION_CONNECTION_TIMEOUT_IN_MILLISECONDS")).map(Long::parseLong).orElse(5000L),
                Optional.ofNullable(System.getenv("WEBMENTION_ISSUER")).orElseThrow(() -> new IllegalStateException("WEBMENTION_ISSUER must be set")),
                Optional.ofNullable(System.getenv("WEBMENTION_JWT_SECRET")).orElseThrow(() -> new IllegalStateException("WEBMENTION_JWT_SECRET must be set")),
                Optional.ofNullable(System.getenv("WEBMENTION_ACCESS_TOKEN_VALIDITY_SECONDS")).map(Long::parseLong).orElse(3600L),
                Optional.ofNullable(System.getenv("WEBMENTION_MANAGEMENT_API_ENABLED")).map("true"::equalsIgnoreCase).orElse(false),
                Optional.ofNullable(System.getenv("WEBMENTION_TEST_MODE")).map("true"::equalsIgnoreCase).orElse(false),
                Optional.ofNullable(System.getenv("WEBMENTION_TEST_PAGES")).map(value -> new HashSet<>(Arrays.asList(value.split(",")))).orElse(new HashSet<>())
        );
    }
}
