package no.clueless.webmention.notifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WebmentionNotifier<TNotification extends WebmentionNotification> {
    @NotNull
    TNotification newNotification(@NotNull String sourceUrl, @NotNull String targetUrl, @Nullable String mentionText);

    void notify(@NotNull TNotification notification);
}
