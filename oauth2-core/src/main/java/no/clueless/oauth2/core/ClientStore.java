package no.clueless.oauth2.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ClientStore {
    @Nullable OAuthClient getClient(@NotNull String clientId);

    @NotNull Set<OAuthClient> getClients(int page, int size, @NotNull String orderByColumn, boolean ascending);

    void disableClient(@NotNull String clientId);

    void enableClient(@NotNull String clientId);

    void deleteClient(@NotNull String clientId);

    void registerClient(@NotNull String clientId, @NotNull String clientSecret, @NotNull Set<String> scopes);

    default boolean authenticate(@NotNull String clientId, @NotNull String providedSecret) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        if (providedSecret.isBlank()) {
            throw new IllegalArgumentException("providedSecret cannot be blank");
        }

        var client = getClient(clientId);
        return client != null && client.isEnabled() && BCrypt.checkpw(providedSecret, client.hashedClientSecret());
    }

    default void seedInitialClient() {
        final var clientId     = Optional.ofNullable(System.getenv("WEBMENTION_INITIAL_CLIENT_ID")).filter(value -> !value.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_INITIAL_CLIENT_ID must be set"));
        final var clientSecret = Optional.ofNullable(System.getenv("WEBMENTION_INITIAL_CLIENT_SECRET")).filter(value -> !value.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_INITIAL_CLIENT_SECRET must be set"));
        final var scopes = Optional.ofNullable(System.getenv("WEBMENTION_INITIAL_CLIENT_SCOPES")).filter(value -> !value.isBlank())
                .map(value -> value.split(","))
                .map(parts -> Arrays.stream(parts).collect(Collectors.toSet()))
                .orElseThrow(() -> new IllegalStateException("WEBMENTION_INITIAL_CLIENT_SCOPES must be set"));
        registerClient(clientId, clientSecret, scopes);
    }

    boolean shouldSeedInitialClient();

    default boolean hasSeedCredentials() {
        var initialClientId     = System.getenv("WEBMENTION_INITIAL_CLIENT_ID");
        var initialClientSecret = System.getenv("WEBMENTION_INITIAL_CLIENT_SECRET");
        var initialClientScopes = System.getenv("WEBMENTION_INITIAL_CLIENT_SCOPES");

        return initialClientId != null &&
                !initialClientId.isBlank() &&
                initialClientSecret != null &&
                !initialClientSecret.isBlank() &&
                initialClientScopes != null &&
                !initialClientScopes.isBlank() && Arrays.stream(initialClientScopes.trim().split("\\s+"))
                .findAny()
                .isPresent();
    }
}
