package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Entity;

@FunctionalInterface
public interface CreateNewEntity<TEntity extends Entity<TId>, TId> {
    /**
     * Creates a new entity based on the given entity.
     *
     * @param entity The entity to base the new entity on. Must not exist in the database.
     * @return The new entity.
     */
    TEntity createNewWithSomethingFromEntity(TEntity entity);
}
