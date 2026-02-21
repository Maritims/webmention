package no.clueless.webmention.persistence;

import java.time.LocalDateTime;

/**
 * Represents an entity in the database.
 *
 * @param <TId> The type of the entity's id.
 */
public interface Entity<TId> {
    /**
     * The entity's unique identifier.
     *
     * @return The entity's unique identifier.
     */
    TId id();

    /**
     * When the entity was created.
     *
     * @return When the entity was created.
     */
    LocalDateTime created();

    /**
     * When the entity was last updated.
     *
     * @return When the entity was last updated.
     */
    LocalDateTime updated();

    /**
     * Updates the entity based on the given entity.
     *
     * @param entity The entity to update the current entity with.
     * @return The updated entity.
     */
    Entity<TId> update(Entity<TId> entity);
}
