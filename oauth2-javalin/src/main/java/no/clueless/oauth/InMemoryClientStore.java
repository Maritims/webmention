package no.clueless.oauth;

import org.mindrot.jbcrypt.BCrypt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    public void registerClient(String clientId, String clientSecret, String... scopes) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }

        var hashedSecret = BCrypt.hashpw(clientSecret, BCrypt.gensalt());
        clients.put(clientId, new OAuthClient(clientId, hashedSecret, scopes == null || scopes.length == 0 ? null : new HashSet<>(List.of(scopes)), true));
    }
}
