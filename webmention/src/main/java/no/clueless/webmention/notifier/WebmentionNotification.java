package no.clueless.webmention.notifier;

public interface WebmentionNotification {
    String sourceUrl();

    String targetUrl();

    String mentionText();
}
