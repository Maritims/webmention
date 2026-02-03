package no.clueless.webmention.http;

import no.clueless.webmention.ContentLengthExceededException;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class LimitedBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
    private final HttpResponse.BodySubscriber<T> downstream;
    private final long                           maxBytes;
    private       long                           bytesReceived = 0;
    private       Flow.Subscription              subscription;

    public LimitedBodySubscriber(HttpResponse.BodySubscriber<T> downstream, long maxBytes) {
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
        for(var item : items) {
            bytesReceived += item.remaining();
        }

        if (bytesReceived > maxBytes) {
            subscription.cancel();
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
