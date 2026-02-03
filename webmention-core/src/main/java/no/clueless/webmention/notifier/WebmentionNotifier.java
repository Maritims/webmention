package no.clueless.webmention.notifier;

public interface WebmentionNotifier<TNotification extends WebmentionNotification> {
    TNotification newNotification(String sourceUrl, String targetUrl, String mentionText);

    void notify(TNotification notification);
}
