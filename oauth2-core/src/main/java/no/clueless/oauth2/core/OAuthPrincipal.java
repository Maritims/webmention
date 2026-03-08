package no.clueless.oauth2.core;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

public record OAuthPrincipal(@NotNull String clientId, @NotNull Set<String> scopes, @NotNull String grantType) {
    public OAuthPrincipal {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        if (grantType.isBlank()) {
            throw new IllegalArgumentException("grantType cannot be blank");
        }
    }
    
    public boolean hasRequiredScope(@NotNull String... requiredScopes) {
        return requiredScopes.length == 0 || Arrays.stream(requiredScopes).anyMatch(scopes::contains);
    }
}
