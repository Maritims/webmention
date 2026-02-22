package no.clueless.webmention.api_client;

import org.jetbrains.annotations.NotNull;

public record Pagination(int page, int size, @NotNull String orderByColumn, @NotNull String orderByDirection) {
    public Pagination {
        if(page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if(size < 1) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if (orderByColumn.isBlank()) {
            throw new IllegalArgumentException("orderByColumn cannot be blank");
        }
        if(orderByDirection.isBlank()) {
            throw new IllegalArgumentException("orderByDirection cannot be blank");
        }
    }

    public Pagination(int page, int size) {
        this(page, size, "created", "desc");
    }

    public Pagination() {
        this(0, 100);
    }

    @NotNull
    public String toQueryString(
            @NotNull String pageKey,
            @NotNull String sizeKey,
            @NotNull String orderByColumnKey,
            @NotNull String orderByDirectionKey
    ) {
        if (pageKey.isBlank() || sizeKey.isBlank() || orderByColumnKey.isBlank() || orderByDirectionKey.isBlank()) {
            throw new IllegalArgumentException("All parameters must be non-blank");
        }
        return String.format("%s=%d&%s=%d&%s=%s&%s=%s", pageKey, page, sizeKey, size, orderByColumnKey, orderByColumn, orderByDirectionKey, orderByDirection);
    }

    @NotNull
    public String toQueryString() {
        return toQueryString("page", "size", "orderByColumn", "orderByDirection");
    }
}
