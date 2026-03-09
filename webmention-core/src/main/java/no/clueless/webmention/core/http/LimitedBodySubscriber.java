package no.clueless.webmention.core.http;

import no.clueless.webmention.core.ContentLengthExceededException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * A body subscriber that limits the number of bytes received.
 *
 * @param <T> the type of the body
 */
public class LimitedBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
    @NotNull
    private final HttpResponse.BodySubscriber<T> downstream;
    private final long                           maxBytes;
    private       long                           bytesReceived = 0;
    @Nullable
    private       Flow.Subscription              subscription;

    /**
     * Constructor.
     *
     * @param downstream the downstream subscriber
     * @param maxBytes   the maximum number of bytes to receive
     */
    public LimitedBodySubscriber(HttpResponse.@NotNull BodySubscriber<T> downstream, long maxBytes) {
        this.downstream = downstream;
        this.maxBytes   = maxBytes;
    }

    @Override
    public CompletionStage<T> getBody() {
        return downstream.getBody();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        downstream.onSubscribe(subscription);
    }

    @Override
    public void onNext(List<ByteBuffer> items) {
        for (var item : items) {
            bytesReceived += item.remaining();
        }

        if (bytesReceived > maxBytes) {
            if(subscription != null) {
                subscription.cancel();
            }
            downstream.onError(new ContentLengthExceededException("Content length exceeded limit of " + maxBytes + " bytes"));
        } else {
            downstream.onNext(items);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        downstream.onError(throwable);
    }

    @Override
    public void onComplete() {
        downstream.onComplete();
    }
}
