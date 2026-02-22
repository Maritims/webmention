package no.clueless.webmention.notifier.email;

import no.clueless.webmention.notifier.WebmentionNotification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record WebmentionEmailNotification(
        @NotNull String senderEmailAddress,
        @NotNull String recipientEmailAddress,
        @NotNull String subject,
        @NotNull String body,
        @NotNull String sourceUrl,
        @NotNull String targetUrl,
        @Nullable String mentionText
) implements WebmentionNotification {
    public WebmentionEmailNotification {
        if (senderEmailAddress.isBlank()) {
            throw new IllegalArgumentException("senderEmailAddress cannot be blank");
        }
        if (recipientEmailAddress.isBlank()) {
            throw new IllegalArgumentException("recipientEmailAddress cannot be blank");
        }
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject cannot be blank");
        }
        if (body.isBlank()) {
            throw new IllegalArgumentException("body cannot be blank");
        }
        if (sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl cannot be blank");
        }
        if (targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl cannot be blank");
        }
    }
}
