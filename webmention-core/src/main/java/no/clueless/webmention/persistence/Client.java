package no.clueless.webmention.persistence;

import java.time.LocalDateTime;

public record Client(Integer id, String clientId, String clientSecret, boolean isEnabled, LocalDateTime created, LocalDateTime updated) implements Entity<Integer> {
    public Client {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }
    }

    public Client update(String clientSecret, boolean isEnabled, LocalDateTime updated) {
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }
        if (updated == null) {
            throw new IllegalArgumentException("updated cannot be null");
        }
        return new Client(id, clientId, clientSecret, isEnabled, created, updated);
    }

    public static Client newClient(String clientId, String clientSecret) {
        return new Client(null, clientId, clientSecret, true, LocalDateTime.now(), LocalDateTime.now());
    }

    public static Client existingClient(Integer id, String clientId, String clientSecret, boolean isEnabled, LocalDateTime created, LocalDateTime updated) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id cannot be null or less than 1");
        }
        if (created == null) {
            throw new IllegalArgumentException("created cannot be null");
        }
        if (updated == null) {
            throw new IllegalArgumentException("updated cannot be null");
        }

        return new Client(id, clientId, clientSecret, isEnabled, created, updated);
    }

    @Override
    public Entity<Integer> update(Entity<Integer> entity) {
        var client = (Client) entity;
        return update(client.clientSecret, client.isEnabled, client.updated);
    }
}
