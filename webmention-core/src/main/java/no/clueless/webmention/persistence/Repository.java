package no.clueless.webmention.persistence;

import no.clueless.webmention.persistence.sqlite.CreateNewEntity;
import no.clueless.webmention.persistence.sqlite.FindExistingEntity;
import no.clueless.webmention.persistence.sqlite.UpdateExistingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Repository<TEntity extends Entity<TId>, TId> {
    @NotNull
    Repository<TEntity, TId> initialize();

    @NotNull
    default String getOrderByColumn() {
        return "id";
    }

    @NotNull
    default String getOrderByDirection() {
        return "desc";
    }

    @NotNull
    Integer count();

    @NotNull
    Optional<TEntity> findById(@NotNull TId id);

    @NotNull
    TEntity create(@NotNull TEntity entity);

    @NotNull
    TEntity update(@NotNull TEntity entity);

    @NotNull
    TEntity upsert(@NotNull TEntity entityWithChanges, @NotNull FindExistingEntity<TEntity, TId> findExistingEntity, @NotNull CreateNewEntity<TEntity, TId> createNewEntity, @NotNull UpdateExistingEntity<TEntity, TId> updateExistingEntity);
}
