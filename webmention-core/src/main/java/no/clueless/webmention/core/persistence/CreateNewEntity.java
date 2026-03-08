package no.clueless.webmention.core.persistence;

import org.jetbrains.annotations.NotNull;

/**
 * Creates a new entity based on an existing entity.
 *
 * @param <TEntity> The type of entity to create.
 * @param <TId>     The type of the entity's id.
 */
@FunctionalInterface
public interface CreateNewEntity<TEntity extends Entity<TId>, TId> {
    /**
     * Creates a new entity based on the given entity.
     *
     * @param entity The entity to base the new entity on. Must not exist in the database.
     * @return The new entity.
     */
    @NotNull
    TEntity createNewWithSomethingFromEntity(@NotNull TEntity entity);
}
