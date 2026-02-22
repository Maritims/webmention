package no.clueless.webmention.event;

import no.clueless.webmention.notifier.WebmentionNotification;
import no.clueless.webmention.notifier.WebmentionNotifier;
import no.clueless.webmention.persistence.Webmention;
import no.clueless.webmention.persistence.WebmentionRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Flow;

public class WebmentionReceivedSubscriber<TNotification extends WebmentionNotification> implements Flow.Subscriber<WebmentionEvent> {
    private static final Logger                            log = LoggerFactory.getLogger(WebmentionReceivedSubscriber.class);
    @NotNull
    private final        WebmentionRepository              webmentionRepository;
    @NotNull
    private final        WebmentionNotifier<TNotification> webmentionNotifier;

    public WebmentionReceivedSubscriber(@NotNull WebmentionRepository webmentionRepository, @NotNull WebmentionNotifier<TNotification> webmentionNotifier) {
        this.webmentionRepository = webmentionRepository;
        this.webmentionNotifier   = webmentionNotifier;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription cannot be null").request(1);
    }

    @Override
    public void onNext(@NotNull WebmentionEvent item) {
        var webmention = Webmention.newWebmention(item.sourceUrl(), item.targetUrl(), item.mentionText());
        webmentionRepository.upsert(
                webmention,
                entity -> webmentionRepository.findWebmentionBySourceUrl(item.sourceUrl()).orElseThrow(() -> new IllegalArgumentException(String.format("No webmention found for sourceUrl %s", item.sourceUrl()))),
                entity -> Webmention.newWebmention(item.sourceUrl(), item.targetUrl(), item.mentionText()),
                (entityToUpdate, entityWithChanges) -> (Webmention) entityToUpdate.update(entityWithChanges)
        );
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
