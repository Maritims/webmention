package no.clueless.webmention.persistence;

import java.time.LocalDateTime;

public record Webmention(Integer id, boolean isApproved, String sourceUrl, String targetUrl, String mentionText,
                         LocalDateTime created, LocalDateTime updated) {
    public Webmention {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl cannot be null or blank");
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl cannot be null or blank");
        }
    }

    public Webmention update(boolean isApproved, String mentionText, LocalDateTime updated) {
        return new Webmention(id, isApproved, sourceUrl, targetUrl, mentionText, created, updated);
    }

    public static Webmention newWebmention(String sourceUrl, String targetUrl, String mentionText) {
        return new Webmention(null, false, sourceUrl, targetUrl, mentionText, null, null);
    }

    public static Webmention existingWebmention(Integer id, boolean isApproved, String sourceUrl, String targetUrl, String mentionText, LocalDateTime created, LocalDateTime updated) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id cannot be null or less than 1");
        }
        if (created == null) {
            throw new IllegalArgumentException("created cannot be null");
        }
        if (updated == null) {
            throw new IllegalArgumentException("updated cannot be null");
        }

        return new Webmention(id, isApproved, sourceUrl, targetUrl, mentionText, created, updated);
    }
}
