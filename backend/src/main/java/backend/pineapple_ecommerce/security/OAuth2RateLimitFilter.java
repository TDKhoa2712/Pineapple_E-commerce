package backend.pineapple_ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OAuth2RateLimitFilter — giới hạn số lần initiate OAuth2 flow từ một IP.
 *
 * Về đề xuất Rate Limiting:
 * - Đây là implementation in-memory đơn giản, phù hợp cho single-instance
 * - Production với nhiều instance: nên dùng Redis + Bucket4j hoặc Resilience4j
 * - Limit: 10 request/phút/IP cho OAuth2 authorization endpoint
 *
 * Tại sao cần rate limit OAuth2?
 * - Mỗi OAuth2 initiation tạo session + state parameter
 * - Attacker có thể flood server với OAuth2 requests để làm cạn session memory
 * - CSRF protection của OAuth2 state không bảo vệ được DoS dạng này
 *
 * Lưu ý: Filter này là OPTIONAL — bổ sung vào SecurityConfig nếu cần:
 * .addFilterBefore(new OAuth2RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
 *
 * Nếu đã có API Gateway / Nginx rate limiting ở tầng trên thì không cần filter này.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2RateLimitFilter extends OncePerRequestFilter {

    private final RequestIpResolver ipResolver;

    private static final int    MAX_REQUESTS_PER_MINUTE = 10;
    private static final long   WINDOW_MS               = 60_000L; // 1 phút

    // IP → (count, windowStart)
    private final ConcurrentHashMap<String, long[]> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Chỉ apply cho OAuth2 initiation endpoint
        return !request.getRequestURI().startsWith("/oauth2/authorization/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);

        if (isRateLimited(clientIp)) {
            log.warn("OAuth2 rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"error":"TOO_MANY_REQUESTS",
                     "message":"Quá nhiều yêu cầu đăng nhập. Vui lòng thử lại sau 1 phút."}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        long now = Instant.now().toEpochMilli();

        long[] state = requestCounts.compute(ip, (key, existing) -> {
            if (existing == null || now - existing[1] > WINDOW_MS) {
                return new long[]{1, now};  // Reset window
            }
            existing[0]++;
            return existing;
        });

        return state[0] > MAX_REQUESTS_PER_MINUTE;
    }

    private String getClientIp(HttpServletRequest request) {
        return ipResolver.resolveClientIp(request);
    }
}
