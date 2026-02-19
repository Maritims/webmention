package no.clueless.oauth;

import java.util.Set;

public record OAuthClient(String clientId, String hashedClientSecret, Set<String> scopes, boolean isEnabled) {
    public OAuthClient {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (hashedClientSecret == null || hashedClientSecret.isBlank()) {
            throw new IllegalArgumentException("hashedClientSecret cannot be null or blank");
        }
    }
}
