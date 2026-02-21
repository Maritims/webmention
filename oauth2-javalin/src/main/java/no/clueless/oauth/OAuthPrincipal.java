package no.clueless.oauth;

import java.util.Set;

/**
 * Represents an authenticated principal.
 *
 * @param clientId  the client id
 * @param scopes    the client scopes
 * @param grantType the grant type
 */
public record OAuthPrincipal(String clientId, Set<String> scopes, String grantType) {
    /**
     * Constructor.
     *
     * @param clientId  the client id
     * @param scopes    the client scopes
     * @param grantType the grant type
     * @throws IllegalArgumentException if clientId or grantType is null or blank.
     */
    public OAuthPrincipal {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (grantType == null || grantType.isBlank()) {
            throw new IllegalArgumentException("grantType cannot be null or blank");
        }
    }
}
