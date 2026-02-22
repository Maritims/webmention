package no.clueless.oauth;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

public record OAuthPrincipal(@NotNull String clientId, @NotNull Set<Scope> scopes, @NotNull String grantType) {
    public OAuthPrincipal {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        if (grantType.isBlank()) {
            throw new IllegalArgumentException("grantType cannot be blank");
        }
    }
    
    public boolean hasRequiredScope(@NotNull Scope... requiredScopes) {
        return requiredScopes.length == 0 || Arrays.stream(requiredScopes).anyMatch(scopes::contains);
    }
}
