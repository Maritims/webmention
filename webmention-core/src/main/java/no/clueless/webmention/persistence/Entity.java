package no.clueless.webmention.persistence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public interface Entity<TId> {
    @Nullable
    TId id();

    @Nullable
    LocalDateTime created();

    @Nullable
    LocalDateTime updated();

    @NotNull
    Entity<TId> update(@NotNull Entity<TId> entity);
}
