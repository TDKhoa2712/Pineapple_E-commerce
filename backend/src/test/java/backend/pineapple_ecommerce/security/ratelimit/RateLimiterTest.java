package backend.pineapple_ecommerce.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    private InMemoryRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryRateLimiter();
    }

    @Test
    void testRateLimitingAllowedRequests() {
        String key = "test_ip_1";
        
        // Allow up to 3 requests in 10 seconds
        assertTrue(rateLimiter.isAllowed(key, 3, 10));
        assertTrue(rateLimiter.isAllowed(key, 3, 10));
        assertTrue(rateLimiter.isAllowed(key, 3, 10));
        
        // 4th request should be blocked
        assertFalse(rateLimiter.isAllowed(key, 3, 10));
    }

    @Test
    void testRateLimitingExpiryAndReset() throws InterruptedException {
        String key = "test_ip_2";
        
        assertTrue(rateLimiter.isAllowed(key, 2, 1));
        assertTrue(rateLimiter.isAllowed(key, 2, 1));
        assertFalse(rateLimiter.isAllowed(key, 2, 1)); // Blocked
        
        // Wait for window to expire
        Thread.sleep(1100);
        
        // Should be allowed again
        assertTrue(rateLimiter.isAllowed(key, 2, 1));
    }

    @Test
    void testConcurrencyRateLimiting() throws InterruptedException {
        String key = "test_ip_concurrent";
        int totalThreads = 10;
        int maxRequests = 5;
        
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                boolean allowed = rateLimiter.isAllowed(key, maxRequests, 60);
                if (allowed) {
                    allowedCount.incrementAndGet();
                } else {
                    blockedCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(allowedCount.get() <= maxRequests, "Allowed requests exceed the maximum limit of " + maxRequests);
        assertTrue(blockedCount.get() >= totalThreads - maxRequests, "Blocked requests count is less than expected");
    }
}
