package backend.pineapple_ecommerce.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class InMemoryRateLimiter implements RateLimiter {

    // Cache to hold timestamps of requests. Auto-cleans up key if no requests within 1 hour.
    private final Cache<String, Queue<Long>> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    @Override
    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000);

        Queue<Long> timestamps = cache.get(key, k -> new ConcurrentLinkedQueue<>());
        
        if (timestamps == null) {
            return true;
        }

        synchronized (timestamps) {
            // Remove old timestamps outside of the window
            while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
                timestamps.poll();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.offer(now);
                return true;
            }
            return false;
        }
    }
}
