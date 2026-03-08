package no.clueless.oauth2.core;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TokenValidator {
    @NotNull
    OAuthPrincipal validate(@NotNull String token);
}
