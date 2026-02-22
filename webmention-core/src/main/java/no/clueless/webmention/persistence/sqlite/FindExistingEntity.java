package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds an existing entity based on something from the given entity.
 *
 * @param <TEntity> the type of entity to find
 * @param <TId>     the type of the entity's id
 */
@FunctionalInterface
public interface FindExistingEntity<TEntity extends Entity<TId>, TId> {
    /**
     * Finds an existing entity based on something from the given entity.
     *
     * @param entity The entity to base the search on. Can exist in the database but does not have to.
     * @return An existing entity based on the given entity, or empty if no such entity exists.
     */
    @Nullable
    TEntity findBySomethingFromEntity(@NotNull TEntity entity);
}
