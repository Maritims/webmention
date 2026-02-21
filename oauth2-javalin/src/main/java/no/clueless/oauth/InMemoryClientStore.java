package no.clueless.oauth;

import org.mindrot.jbcrypt.BCrypt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * An in-memory implementation of the ClientStore interface.
 */
public class InMemoryClientStore implements ClientStore {
    private final Map<String, OAuthClient> clients = new ConcurrentHashMap<>();

    @Override
    public OAuthClient getClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        return clients.get(clientId);
    }

    @Override
    public void registerClient(String clientId, String clientSecret, Set<Scope> scopes) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }

        var hashedSecret = BCrypt.hashpw(clientSecret, BCrypt.gensalt());
        clients.put(clientId, new OAuthClient(clientId, hashedSecret, scopes.stream().map(Scope::getLabel).collect(Collectors.toSet()), true));
    }

    @Override
    public boolean shouldSeedInitialClient() {
        return clients.entrySet()
                .stream()
                .noneMatch(entry -> entry.getValue().isEnabled() && entry.getValue().scopes().contains(Scope.WEBMENTIONS_MANAGE.getLabel()));
    }
}
