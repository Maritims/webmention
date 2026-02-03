package no.clueless.webmention.event;

public record WebmentionEvent(String sourceUrl, String targetUrl, String mentionText) {
    public WebmentionEvent {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl cannot be null or blank");
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl cannot be null or blank");
        }
    }
}
