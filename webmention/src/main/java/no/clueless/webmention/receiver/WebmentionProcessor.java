package no.clueless.webmention.receiver;

import no.clueless.webmention.WebmentionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebmentionProcessor {
    private record WebmentionTask(String sourceUrl, String targetUrl) {
        public WebmentionTask {
            if (sourceUrl == null || sourceUrl.isBlank()) {
                throw new IllegalArgumentException("sourceUrl cannot be null or blank");
            }
            if (targetUrl == null || targetUrl.isBlank()) {
                throw new IllegalArgumentException("targetUrl cannot be null or blank");
            }
        }
    }

    private static final Logger                                log       = LoggerFactory.getLogger(WebmentionProcessor.class);
    private static final ConcurrentLinkedQueue<WebmentionTask> queue     = new ConcurrentLinkedQueue<>();
    private final        ScheduledExecutorService              scheduler = Executors.newSingleThreadScheduledExecutor();
    private final        WebmentionRateLimiter                 webmentionRateLimiter;
    private final        WebmentionReceiver                    webmentionReceiver;
    private final        int                                   intervalInSeconds;
    private final        int                                   maxQueueSize;

    /**
     * {@link ConcurrentLinkedQueue#size()} is an O(n) operation. Keep track of the current queue size manually to prevent botltenecking the CPU in the event of heavy load or a DoS attack.
     */
    private final AtomicInteger currentQueueSize = new AtomicInteger(0);

    public WebmentionProcessor(WebmentionRateLimiter webmentionRateLimiter, WebmentionReceiver webmentionReceiver, int intervalInSeconds, int maxQueueSize) {
        if (intervalInSeconds < 1) {
            throw new IllegalArgumentException("intervalInSeconds must be greater than zero");
        }
        if (maxQueueSize < 1) {
            throw new IllegalArgumentException("maxQueueSize must be greater than zero");
        }

        this.webmentionReceiver    = Objects.requireNonNull(webmentionReceiver, "webmentionReceiver cannot be null");
        this.webmentionRateLimiter = Objects.requireNonNull(webmentionRateLimiter, "webmentionRateLimiter cannot be null");
        this.intervalInSeconds     = intervalInSeconds;
        this.maxQueueSize          = maxQueueSize;
    }

    private void processNext() {
        try {
            var task = queue.poll();
            if (task == null) {
                // The queue is empty.
                return;
            }
            currentQueueSize.decrementAndGet();

            log.info("Processing queued mention: {} -> {}", task.sourceUrl(), task.targetUrl());
            webmentionReceiver.receive(task.sourceUrl(), task.targetUrl());
        } catch (WebmentionException e) {
            log.error("Failed to process mention", e);
        } catch (Throwable t) {
            log.error("Unexpected error in thread. Scheduler is still alive", t);
        }
    }

    public void queue(String sourceUrl, String targetUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl cannot be null or blank");
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl cannot be null or blank");
        }

        if (!webmentionRateLimiter.isAllowed(sourceUrl)) {
            log.warn("Rate limit exceeded for sourceUrl {}", sourceUrl);
            return;
        }

        if (currentQueueSize.get() >= maxQueueSize) {
            log.warn("The queue is full! Dropping mention from {}", sourceUrl);
            return;
        }

        queue.add(new WebmentionTask(sourceUrl, targetUrl));
        currentQueueSize.incrementAndGet();
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::processNext, 0, intervalInSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    public static class Builder {
        private WebmentionRateLimiter rateLimiter;
        private WebmentionReceiver    receiver;
        private int                   intervalInSeconds = 5;
        private int                   maxQueueSize      = 5000;

        private Builder() {
        }

        public Builder rateLimiter(WebmentionRateLimiter rateLimiter) {
            this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter cannot be null");
            return this;
        }

        public Builder receiver(WebmentionReceiver receiver) {
            this.receiver = Objects.requireNonNull(receiver, "receiver cannot be null");
            return this;
        }

        public Builder intervalInSeconds(int intervalInSeconds) {
            this.intervalInSeconds = intervalInSeconds;
            return this;
        }

        public Builder maxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            return this;
        }

        public WebmentionProcessor build() {
            return new WebmentionProcessor(rateLimiter, receiver, intervalInSeconds, maxQueueSize);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
