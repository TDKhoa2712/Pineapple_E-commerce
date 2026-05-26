package backend.pineapple_ecommerce.security.ratelimit;

import backend.pineapple_ecommerce.common.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = getClientIp(request);
        String endpoint = getEndpointName(joinPoint);

        String identifier = extractIdentifier(joinPoint.getArgs());

        boolean ipAllowed = true;
        boolean identifierAllowed = true;

        // Check IP rate limit
        if (rateLimit.type() == RateLimitType.IP || rateLimit.type() == RateLimitType.IP_AND_EMAIL) {
            String ipKey = "rate_limit:ip:" + ip + ":" + endpoint;
            ipAllowed = rateLimiter.isAllowed(ipKey, rateLimit.maxRequests(), rateLimit.windowSeconds());
        }

        // Check Email/Identifier rate limit
        if ((rateLimit.type() == RateLimitType.EMAIL || rateLimit.type() == RateLimitType.IP_AND_EMAIL)
                && identifier != null && !identifier.isBlank()) {
            String idKey = "rate_limit:email:" + identifier + ":" + endpoint;
            identifierAllowed = rateLimiter.isAllowed(idKey, rateLimit.maxRequests(), rateLimit.windowSeconds());
        }

        if (!ipAllowed) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", ip, endpoint);
            throw new RateLimitException("Quá nhiều yêu cầu từ địa chỉ IP này. Vui lòng thử lại sau.");
        }

        if (!identifierAllowed) {
            log.warn("Rate limit exceeded for email/username: {} on endpoint: {}", identifier, endpoint);
            throw new RateLimitException("Quá nhiều yêu cầu cho tài khoản này. Vui lòng thử lại sau.");
        }

        return joinPoint.proceed();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getEndpointName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private String extractIdentifier(Object[] args) {
        // 1. Try to extract from request DTO via reflection
        for (Object arg : args) {
            if (arg == null) continue;
            try {
                Method getEmailMethod = arg.getClass().getMethod("getEmail");
                String email = (String) getEmailMethod.invoke(arg);
                if (email != null && !email.isBlank()) {
                    return email;
                }
            } catch (NoSuchMethodException e) {
                try {
                    Method getUsernameMethod = arg.getClass().getMethod("getUsername");
                    String username = (String) getUsernameMethod.invoke(arg);
                    if (username != null && !username.isBlank()) {
                        return username;
                    }
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }

        // 2. Fallback to authenticated user principal
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }

        return null;
    }
}
