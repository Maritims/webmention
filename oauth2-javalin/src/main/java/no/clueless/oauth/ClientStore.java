package no.clueless.oauth;

import org.mindrot.jbcrypt.BCrypt;

public interface ClientStore {
    OAuthClient getClient(String clientId);

    void registerClient(String clientId, String clientSecret, String... scopes);

    default boolean authenticate(String clientId, String providedSecret) {
        if(clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if(providedSecret == null || providedSecret.isBlank()) {
            throw new IllegalArgumentException("providedSecret cannot be null or blank");
        }

        var client = getClient(clientId);
        return client != null && client.isEnabled() && BCrypt.checkpw(providedSecret, client.hashedClientSecret());
    }
}
