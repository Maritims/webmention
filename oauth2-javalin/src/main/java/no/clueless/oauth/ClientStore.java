package no.clueless.oauth;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Set;

public interface ClientStore {
    /**
     * Returns the OAuthClient with the given clientId, or null if no such client exists.
     *
     * @param clientId the client id to look up
     * @return the OAuthClient with the given clientId, or null if no such client exists.
     */
    OAuthClient getClient(String clientId);

    /**
     * Disables the OAuthClient with the given clientId.
     * @param clientId the client id to disable
     */
    void disableClient(String clientId);

    /**
     * Enables the OAuthClient with the given clientId.
     * @param clientId the client id to enable
     */
    void enableClient(String clientId);

    /**
     * Deletes the OAuthClient with the given clientId.
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
     * Seeds the client store with an initial client..
     *
     * @param clientId     the client id
     * @param clientSecret the client secret
     * @param scopes       the client scopes
     * @throws IllegalArgumentException if clientId, clientSecret or scopes is null or empty.
     */
    default void seedInitialClient(String clientId, String clientSecret, Set<Scope> scopes) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("scopes cannot be null or empty");
        }

        registerClient(clientId, clientSecret, scopes);
    }

    /**
     * Checks if the client store should seed an initial client.
     *
     * @return true if the client store should seed an initial client, false otherwise.
     */
    boolean shouldSeedInitialClient();
}
