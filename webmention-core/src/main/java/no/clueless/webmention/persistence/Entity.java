package no.clueless.webmention.persistence;

import java.time.LocalDateTime;

public interface Entity<TId> {
    TId id();

    LocalDateTime created();

    LocalDateTime updated();

    Entity<TId> update(Entity<TId> entity);
}
