package no.clueless.oauth;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

@FunctionalInterface
public interface TokenGenerator {
    @NotNull
    String generate(@NotNull OAuthClient client, @NotNull Set<Scope> scopes);
}
