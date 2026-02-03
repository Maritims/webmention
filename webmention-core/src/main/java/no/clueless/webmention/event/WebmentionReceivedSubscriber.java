package no.clueless.webmention.event;

import no.clueless.webmention.notifier.WebmentionNotification;
import no.clueless.webmention.notifier.WebmentionNotifier;
import no.clueless.webmention.persistence.Webmention;
import no.clueless.webmention.persistence.WebmentionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Flow;

public class WebmentionReceivedSubscriber<TNotification extends WebmentionNotification> implements Flow.Subscriber<WebmentionEvent> {
    private static final Logger                            log = LoggerFactory.getLogger(WebmentionReceivedSubscriber.class);
    private final        WebmentionRepository<?>           webmentionRepository;
    private final        WebmentionNotifier<TNotification> webmentionNotifier;

    public WebmentionReceivedSubscriber(WebmentionRepository<?> webmentionRepository, WebmentionNotifier<TNotification> webmentionNotifier) {
        this.webmentionRepository = webmentionRepository;
        this.webmentionNotifier   = webmentionNotifier;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription cannot be null").request(1);
    }

    @Override
    public void onNext(WebmentionEvent item) {
        var webmention = Webmention.newWebmention(item.sourceUrl(), item.targetUrl(), item.mentionText());
        webmentionRepository.upsertWebmention(webmention);
        var notification = webmentionNotifier.newNotification(item.sourceUrl(), item.targetUrl(), item.mentionText());
        webmentionNotifier.notify(notification);
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Error in webmention subscription", throwable);
    }

    @Override
    public void onComplete() {
        log.info("Webmention subscription completed");
    }
}
