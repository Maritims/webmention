package no.clueless.webmention.core.persistence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface WebmentionRepository extends Repository<Webmention, Integer> {
    @NotNull
    Optional<Webmention> findWebmentionBySourceUrl(@NotNull String sourceUrl);

    void updateApproval(@NotNull Webmention webmention, boolean isApproved);

    void deleteWebmention(int id);

    @NotNull
    List<Webmention> getWebmentionsByIsApproved(int pageNumber, int pageSize, @NotNull String orderByColumn, @NotNull String orderDirection, @Nullable Boolean isApproved);
}
