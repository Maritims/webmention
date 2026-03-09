package no.clueless.webmention.core.receiver;

import no.clueless.webmention.core.UnexpectedStatusCodeException;
import no.clueless.webmention.core.WebmentionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebmentionProcessor {
    private record WebmentionTask(@NotNull String sourceUrl, @NotNull String targetUrl) {
        public WebmentionTask {
            if (sourceUrl.isBlank()) {
                throw new IllegalArgumentException("sourceUrl cannot be blank");
            }
            if (targetUrl.isBlank()) {
                throw new IllegalArgumentException("targetUrl cannot be blank");
            }
        }
    }

    private static final Logger                                log       = LoggerFactory.getLogger(WebmentionProcessor.class);
    private static final ConcurrentLinkedQueue<WebmentionTask> queue     = new ConcurrentLinkedQueue<>();
    private final        ScheduledExecutorService              scheduler = Executors.newSingleThreadScheduledExecutor();
    @NotNull
    private final        WebmentionRateLimiter                 webmentionRateLimiter;
    @NotNull
    private final        WebmentionReceiver                    webmentionReceiver;
    private final        int                                   intervalInSeconds;
    private final        int                                   maxQueueSize;

    /**
     * {@link ConcurrentLinkedQueue#size()} is an O(n) operation. Keep track of the current queue size manually to prevent botltenecking the CPU in the event of heavy load or a DoS attack.
     */
    private final AtomicInteger currentQueueSize = new AtomicInteger(0);

    public WebmentionProcessor(@NotNull WebmentionRateLimiter webmentionRateLimiter, @NotNull WebmentionReceiver webmentionReceiver, int intervalInSeconds, int maxQueueSize) {
        if (intervalInSeconds < 1) {
            throw new IllegalArgumentException("intervalInSeconds must be greater than zero");
        }
        if (maxQueueSize < 1) {
            throw new IllegalArgumentException("maxQueueSize must be greater than zero");
        }

        this.webmentionReceiver    = webmentionReceiver;
        this.webmentionRateLimiter = webmentionRateLimiter;
        this.intervalInSeconds     = intervalInSeconds;
        this.maxQueueSize          = maxQueueSize;
    }

    public WebmentionProcessor(@NotNull WebmentionRateLimiter webmentionRateLimiter, @NotNull WebmentionReceiver webmentionReceiver) {
        this(webmentionRateLimiter, webmentionReceiver, 5, 5000);
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
        } catch (UnexpectedStatusCodeException e) {
            log.warn("Failed to process mention: {}", e.getMessage());
        } catch (WebmentionException e) {
            log.error("Failed to process mention: {}", e.getMessage());
        } catch (Throwable t) {
            log.error("Unexpected error in thread. Scheduler is still alive", t);
        }
    }

    public void queue(@NotNull String sourceUrl, @NotNull String targetUrl) {
        if (sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl cannot be blank");
        }
        if (targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl cannot be blank");
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
}
