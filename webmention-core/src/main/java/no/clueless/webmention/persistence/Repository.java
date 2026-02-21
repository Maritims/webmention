package no.clueless.webmention.persistence;

import no.clueless.webmention.persistence.sqlite.CreateNewEntity;
import no.clueless.webmention.persistence.sqlite.FindExistingEntity;
import no.clueless.webmention.persistence.sqlite.UpdateExistingEntity;

import java.util.Optional;

/**
 * A repository for entities.
 *
 * @param <TEntity> the type of entity stored in the repository
 * @param <TId>     the type of the entity's id
 */
public interface Repository<TEntity extends Entity<TId>, TId> {
    /**
     * Initializes the repository.
     *
     * @return this repository
     */
    Repository<TEntity, TId> initialize();

    /**
     * Returns the column to order by.
     *
     * @return the column to order by
     */
    default String getOrderByColumn() {
        return "id";
    }

    /**
     * Returns the direction to order by.
     *
     * @return the direction to order by
     */
    default String getOrderByDirection() {
        return "desc";
    }

    /**
     * Returns the number of entities in the repository.
     *
     * @return the number of entities
     */
    Integer count();

    /**
     * Returns an entity by its id.
     *
     * @param id the entity's id
     * @return the entity
     */
    Optional<TEntity> getById(TId id);

    /**
     * Creates a new entity.
     *
     * @param entity the entity to create
     * @return the created entity
     */
    TEntity create(TEntity entity);

    /**
     * Updates an existing entity.
     *
     * @param entity the entity to update
     * @return the updated entity
     */
    TEntity update(TEntity entity);

    /**
     * Upserts an entity.
     *
     * @param entityWithChanges    the entity with changes
     * @param findExistingEntity   find an existing entity based on the entityWithChanges
     * @param createNewEntity      create a new entity based on the entityWithChanges
     * @param updateExistingEntity update an existing entity based on the entityWithChanges
     * @return the upserted entity
     */
    TEntity upsert(
            TEntity entityWithChanges,
            FindExistingEntity<TEntity, TId> findExistingEntity,
            CreateNewEntity<TEntity, TId> createNewEntity,
            UpdateExistingEntity<TEntity, TId> updateExistingEntity
    );
}
