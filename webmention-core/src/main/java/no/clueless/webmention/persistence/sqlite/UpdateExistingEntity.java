package no.clueless.webmention.persistence.sqlite;

import no.clueless.webmention.persistence.Entity;

@FunctionalInterface
public interface UpdateExistingEntity<TEntity extends Entity<TId>, TId> {
    /**
     * Updates the entityToUpdate with the changes from entityWithChanges.
     *
     * @param entityToUpdate    The entity to update. Must exist in the database.
     * @param entityWithChanges The entity with the changes. Cannot exist in the database.
     * @return The updated entity.
     */
    TEntity updateWithSomethingFromEntity(TEntity entityToUpdate, TEntity entityWithChanges);
}
