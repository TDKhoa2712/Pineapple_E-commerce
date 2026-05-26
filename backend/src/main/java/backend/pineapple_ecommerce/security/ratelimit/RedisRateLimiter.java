package backend.pineapple_ecommerce.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimitScript;

    @Override
    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000);

        try {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(windowStart),
                    String.valueOf(maxRequests),
                    String.valueOf(windowSeconds)
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            // High reliability fallback: if Redis fails, log the error and allow request
            log.error("Redis rate limit check failed for key: {}. Falling back to ALLOW.", key, e);
            return true;
        }
    }
}
