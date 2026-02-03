package no.clueless.webmention.notifier.email;

import no.clueless.webmention.notifier.WebmentionNotification;

public record WebmentionEmailNotification(String senderEmailAddress, String recipientEmailAddress, String subject,
                                          String body, String sourceUrl, String targetUrl,
                                          String mentionText) implements WebmentionNotification {
    public WebmentionEmailNotification {
        if (senderEmailAddress == null || senderEmailAddress.isBlank()) {
            throw new IllegalArgumentException("senderEmailAddress cannot be null or blank");
        }
        if (recipientEmailAddress == null || recipientEmailAddress.isBlank()) {
            throw new IllegalArgumentException("recipientEmailAddress cannot be null or blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject cannot be null or blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body cannot be null or blank");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl cannot be null or blank");
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl cannot be null or blank");
        }
    }
}
