package no.clueless.webmention.core.notifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WebmentionNotification {
    @NotNull
    String sourceUrl();

    @NotNull
    String targetUrl();

    @Nullable
    String mentionText();
}
