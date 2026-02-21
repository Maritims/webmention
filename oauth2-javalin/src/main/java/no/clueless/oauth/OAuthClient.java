package no.clueless.oauth;

import java.util.Set;

/**
 * Represents an OAuth client.
 */
public record OAuthClient(String clientId, String hashedClientSecret, Set<String> scopes, boolean isEnabled) {
    /**
     * Constructor.
     *
     * @param clientId           the client id
     * @param hashedClientSecret the hashed client secret
     * @param scopes             the client scopes
     * @param isEnabled          whether the client is enabled
     * @throws IllegalArgumentException if clientId or hashedClientSecret is null or blank.
     */
    public OAuthClient {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (hashedClientSecret == null || hashedClientSecret.isBlank()) {
            throw new IllegalArgumentException("hashedClientSecret cannot be null or blank");
        }
    }
}
