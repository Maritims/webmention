package no.clueless.oauth;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ClientStore {
    /**
     * Returns the OAuthClient with the given clientId, or null if no such client exists.
     *
     * @param clientId the client id to look up
     * @return the OAuthClient with the given clientId, or null if no such client exists.
     */
    OAuthClient getClient(String clientId);

    /**
     * Returns a paged list of OAuthClients.
     *
     * @param page          0-based page number.
     * @param size          Page size.
     * @param orderByColumn Column to order by.
     * @param ascending     Whether to order ascending or descending.
     * @return A paged list of OAuthClients.
     */
    Set<OAuthClient> getClients(int page, int size, String orderByColumn, boolean ascending);

    /**
     * Disables the OAuthClient with the given clientId.
     *
     * @param clientId the client id to disable
     */
    void disableClient(String clientId);

    /**
     * Enables the OAuthClient with the given clientId.
     *
     * @param clientId the client id to enable
     */
    void enableClient(String clientId);

    /**
     * Deletes the OAuthClient with the given clientId.
     *
     * @param clientId the client id to delete
     */
    void deleteClient(String clientId);

    /**
     * Registers a new OAuthClient with the given clientId and clientSecret.
     *
     * @param clientId     the client id
     * @param clientSecret the client secret
     * @param scopes       the client scopes
     */
    void registerClient(String clientId, String clientSecret, Set<Scope> scopes);

    /**
     * Authenticates the given clientId and clientSecret.
     *
     * @param clientId       the client id
     * @param providedSecret the client secret provided by the client
     * @return True if the client is authenticated, false otherwise.
     * @throws IllegalArgumentException if clientId or providedSecret is null or blank.
     */
    default boolean authenticate(String clientId, String providedSecret) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (providedSecret == null || providedSecret.isBlank()) {
            throw new IllegalArgumentException("providedSecret cannot be null or blank");
        }

        var client = getClient(clientId);
        return client != null && client.isEnabled() && BCrypt.checkpw(providedSecret, client.hashedClientSecret());
    }

    /**
     * Seeds the client store with an initial client.
     */
    default void seedInitialClient() {
        final var clientId     = Optional.ofNullable(System.getenv("WEBMENTION_INITIAL_CLIENT_ID")).filter(value -> !value.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_INITIAL_CLIENT_ID must be set"));
        final var clientSecret = Optional.ofNullable(System.getenv("WEBMENTION_INITIAL_CLIENT_SECRET")).filter(value -> !value.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_INITIAL_CLIENT_SECRET must be set"));
        final var scopes = Optional.ofNullable(System.getenv("WEBMENTION_INITIAL_CLIENT_SCOPES")).filter(value -> !value.isBlank())
                .map(Scope::fromLabel)
                .map(Optional::stream)
                .map(stream -> stream.collect(Collectors.toSet()))
                .orElseThrow(() -> new IllegalStateException("WEBMENTION_INITIAL_CLIENT_SCOPES must be set"));
        registerClient(clientId, clientSecret, scopes);
    }

    /**
     * Checks if the client store should seed an initial client.
     *
     * @return true if the client store should seed an initial client, false otherwise.
     */
    boolean shouldSeedInitialClient();

    /**
     * Checks if the client store has seed credentials.
     *
     * @return true if the client store has seed credentials, false otherwise.
     */
    default boolean hasSeedCredentials() {
        var initialClientId     = System.getenv("WEBMENTION_INITIAL_CLIENT_ID");
        var initialClientSecret = System.getenv("WEBMENTION_INITIAL_CLIENT_SECRET");
        var initialClientScopes = System.getenv("WEBMENTION_INITIAL_CLIENT_SCOPES");

        return initialClientId != null &&
                !initialClientId.isBlank() &&
                initialClientSecret != null &&
                !initialClientSecret.isBlank() &&
                initialClientScopes != null &&
                !initialClientScopes.isBlank() && Arrays.stream(initialClientScopes.trim().split("\\s+"))
                .map(Scope::fromLabel)
                .flatMap(Optional::stream)
                .findAny()
                .isPresent();
    }
}
