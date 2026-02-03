package no.clueless.webmention.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebmentionRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(WebmentionRateLimiter.class);
    private final long cooldownMillis;
    private final Map<String, Long> lruCache;

    public WebmentionRateLimiter(int maxEntries, long cooldownSeconds) {
        this.cooldownMillis = cooldownSeconds * 1000;
        this.lruCache       = Collections.synchronizedMap(new LinkedHashMap<>(maxEntries, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > maxEntries; // Evict oldest when we hit the cap
            }
        });
    }

    public boolean isAllowed(String source) {
        var now      = System.currentTimeMillis();
        var lastSeen = lruCache.get(source);

        if (lastSeen != null && (now - lastSeen) < cooldownMillis) {
            log.debug("Source {} is still in the dog house!", source);
            return false; // Still wearing the cone of shame.
        }

        // We haven't seen this sourceUrl for a while, or maybe ever.
        lruCache.put(source, now);
        return true;
    }

    public static class Builder {
        private int  maxEntries;
        private long cooldownMillis;

        private Builder() {
        }

        public Builder maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        public Builder cooldownMillis(long cooldownMillis) {
            this.cooldownMillis = cooldownMillis;
            return this;
        }

        public WebmentionRateLimiter build() {
            return new WebmentionRateLimiter(maxEntries, cooldownMillis);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
