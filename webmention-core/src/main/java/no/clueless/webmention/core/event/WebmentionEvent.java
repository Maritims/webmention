package no.clueless.webmention.core.event;

import org.jetbrains.annotations.NotNull;

public record WebmentionEvent(@NotNull String sourceUrl, @NotNull String targetUrl, @NotNull String mentionText) {
    public WebmentionEvent {
        if (sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl cannot be blank");
        }
        if (targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl cannot be blank");
        }
    }
}
