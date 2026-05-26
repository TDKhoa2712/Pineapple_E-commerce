package backend.pineapple_ecommerce.security.ratelimit;

public interface RateLimiter {
    /**
     * Checks if a request is allowed under the given rate limit settings.
     *
     * @param key           Unique key for the client/identifier.
     * @param maxRequests   Max requests allowed inside the window.
     * @param windowSeconds Window length in seconds.
     * @return true if request is allowed, false if it should be rate limited.
     */
    boolean isAllowed(String key, int maxRequests, long windowSeconds);
}
