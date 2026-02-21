package no.clueless.webmention.persistence;

import no.clueless.webmention.persistence.sqlite.CreateNewEntity;
import no.clueless.webmention.persistence.sqlite.FindExistingEntity;
import no.clueless.webmention.persistence.sqlite.UpdateExistingEntity;

import java.util.Optional;

public interface Repository<TEntity extends Entity<TId>, TId> {
    Repository<TEntity, TId> initialize();

    default String getOrderByColumn() {
        return "id";
    }

    default String getOrderByDirection() {
        return "desc";
    }

    Integer count();

    Optional<TEntity> getById(TId id);

    TEntity create(TEntity entity);

    TEntity update(TEntity webmention);

    TEntity upsert(
            TEntity entityWithChanges,
            FindExistingEntity<TEntity, TId> findExistingEntity,
            CreateNewEntity<TEntity, TId> createNewEntity,
            UpdateExistingEntity<TEntity, TId> updateExistingEntity
    );
}
