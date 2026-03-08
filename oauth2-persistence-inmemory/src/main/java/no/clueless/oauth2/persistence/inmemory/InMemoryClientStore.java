package no.clueless.oauth2.persistence.inmemory;

import no.clueless.oauth2.core.ClientStore;
import no.clueless.oauth2.core.OAuthClient;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory implementation of the ClientStore interface.
 */
public class InMemoryClientStore implements ClientStore {
    @NotNull
    private final Map<String, OAuthClient> clients = new ConcurrentHashMap<>();

    /**
     * Default constructor.
     */
    public InMemoryClientStore() {
    }

    @Override
    public OAuthClient getClient(@NotNull String clientId) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        return clients.get(clientId);
    }

    @Override
    public @NotNull Set<OAuthClient> getClients(int page, int size, @NotNull String orderByColumn, boolean ascending) {
        return new HashSet<>(clients.values());
    }

    @Override
    public void disableClient(@NotNull String clientId) {
        clients.computeIfPresent(clientId, (k, client) -> new OAuthClient(client.clientId(), client.hashedClientSecret(), client.scopes(), false));
    }

    @Override
    public void enableClient(@NotNull String clientId) {
        clients.computeIfPresent(clientId, (k, client) -> new OAuthClient(client.clientId(), client.hashedClientSecret(), client.scopes(), true));
    }

    @Override
    public void deleteClient(@NotNull String clientId) {
        clients.remove(clientId);
    }

    @Override
    public void registerClient(@NotNull String clientId, @NotNull String clientSecret, @NotNull Set<String> scopes) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        if (clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be blank");
        }

        var hashedSecret = BCrypt.hashpw(clientSecret, BCrypt.gensalt());
        clients.put(clientId, new OAuthClient(clientId, hashedSecret, scopes, true));
    }

    @Override
    public boolean shouldSeedInitialClient() {
        return clients.entrySet()
                .stream()
                .noneMatch(entry -> entry.getValue().isEnabled() && entry.getValue().scopes().contains("webmentions:manage"));
    }
}
