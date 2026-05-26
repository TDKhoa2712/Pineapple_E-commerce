package backend.pineapple_ecommerce.security.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply Rate Limiting on Controller methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "";
    int maxRequests() default 10;
    long windowSeconds() default 60;
    RateLimitType type() default RateLimitType.IP;
}
