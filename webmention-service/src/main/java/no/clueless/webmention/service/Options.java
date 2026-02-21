package no.clueless.webmention.service;

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
        String databaseConnectionString,
        String webmentionEndpoint,
        Set<String> supportedDomains,
        long connectTimeout,
        String oauthIssuer,
        String oauthJwtSecret,
        long oauthAccessTokenValiditySeconds,
        boolean isOauthManagementApiEnabled,
        boolean testMode,
        Set<String> testPages
) {
    public Options {
        if (httpServerPort < 1024 || httpServerPort > 65535) {
            throw new IllegalArgumentException("httpServerPort must be between 1024 and 65535");
        }
        if (databaseConnectionString == null || databaseConnectionString.isBlank()) {
            throw new IllegalArgumentException("databaseConnectionString cannot be null or blank");
        }
        if (webmentionEndpoint == null || webmentionEndpoint.isBlank()) {
            throw new IllegalArgumentException("webmentionEndpoint cannot be null or blank");
        }
        if (supportedDomains == null || supportedDomains.isEmpty()) {
            throw new IllegalArgumentException("supportedDomains cannot be null or empty");
        }
        if (oauthIssuer == null || oauthIssuer.isBlank()) {
            throw new IllegalArgumentException("oauthIssuer cannot be null or blank");
        }
        if (oauthJwtSecret == null || oauthJwtSecret.isBlank()) {
            throw new IllegalArgumentException("oauthJwtSecret cannot be null or blank");
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
