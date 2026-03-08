package no.clueless.oauth2.core;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record OAuthClient(@NotNull String clientId, @NotNull String hashedClientSecret, @NotNull Set<String> scopes, boolean isEnabled) {
    public OAuthClient {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        if (hashedClientSecret.isBlank()) {
            throw new IllegalArgumentException("hashedClientSecret cannot be blank");
        }
    }
}
