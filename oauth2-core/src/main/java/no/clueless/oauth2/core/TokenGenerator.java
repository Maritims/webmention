package no.clueless.oauth2.core;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

@FunctionalInterface
public interface TokenGenerator {
    @NotNull
    String generate(@NotNull OAuthClient client, @NotNull Set<String> scopes);
}
