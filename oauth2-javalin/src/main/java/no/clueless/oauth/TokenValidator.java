package no.clueless.oauth;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TokenValidator {
    @NotNull
    OAuthPrincipal validate(@NotNull String token);
}
