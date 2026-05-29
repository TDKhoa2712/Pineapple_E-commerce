package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.common.config.TrustedProxyProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RequestIpResolver")
class RequestIpResolverTest {

    private RequestIpResolver ipResolver;
    private TrustedProxyProperties properties;

    @BeforeEach
    void setUp() {
        properties = new TrustedProxyProperties();
        // Setup standard trusted proxy ranges
        properties.setTrustedProxies(List.of(
                "127.0.0.1",
                "::1",
                "10.0.0.0/8",
                "192.168.0.0/16"
        ));
        properties.setTrustAllProxies(false);
        ipResolver = new RequestIpResolver(properties);
    }

    @Test
    @DisplayName("Nên chuẩn hóa IPv6 localhost về IPv4 localhost")
    void shouldNormalizeIPv6Localhost() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("::1");

        String ip = ipResolver.resolveClientIp(request);
        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("Nên trả về remoteAddr trực tiếp nếu không phải trusted proxy và không có X-Forwarded-For")
    void shouldReturnRemoteAddrDirectlyIfNoProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");

        String ip = ipResolver.resolveClientIp(request);
        assertThat(ip).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("Nên bỏ qua X-Forwarded-For nếu caller không phải trusted proxy (Chống giả mạo IP)")
    void shouldIgnoreXForwardedForIfCallerNotTrustedProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

        String ip = ipResolver.resolveClientIp(request);
        assertThat(ip).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("Nên đọc X-Forwarded-For nếu caller là trusted proxy")
    void shouldReadXForwardedForIfCallerIsTrustedProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42");

        String ip = ipResolver.resolveClientIp(request);
        assertThat(ip).isEqualTo("203.0.113.42");
    }

    @Test
    @DisplayName("Nên trả về IP client thực sự khi client chèn giả mạo X-Forwarded-For qua Nginx")
    void shouldExtractActualClientIpWhenClientSpoofsXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        // Nginx running on localhost connects to Spring Boot
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        // Malicious client sent "X-Forwarded-For: 1.1.1.1", Nginx appended client actual IP "203.0.113.5"
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1, 203.0.113.5");

        String ip = ipResolver.resolveClientIp(request);
        assertThat(ip).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("Nên trích xuất IP client qua chuỗi nhiều proxy tin cậy (Cloudflare -> AWS ALB -> EC2)")
    void shouldResolveClientIpAcrossMultipleTrustedProxies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        // ALB is in private network (10.0.1.50)
        when(request.getRemoteAddr()).thenReturn("10.0.1.50");
        // X-Forwarded-For: client (203.0.113.5), cloudflare (192.168.1.10)
        // Note: 192.168.1.10 matches "192.168.0.0/16" (trusted proxy)
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 192.168.1.10");

        String ip = ipResolver.resolveClientIp(request);
        assertThat(ip).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("Nên đọc X-Forwarded-For từ bất kỳ caller nào nếu trustAllProxies được bật")
    void shouldTrustAllProxiesIfConfigured() {
        properties.setTrustAllProxies(true);
        RequestIpResolver trustAllResolver = new RequestIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1, 2.2.2.2");

        String ip = trustAllResolver.resolveClientIp(request);
        assertThat(ip).isEqualTo("2.2.2.2");
    }

    @Test
    @DisplayName("Nên xử lý an toàn nếu định dạng IP/CIDR không hợp lệ")
    void shouldHandleInvalidIpFormatsSafely() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("invalid-ip, 203.0.113.5");

        String ip = ipResolver.resolveClientIp(request);
        assertThat(ip).isEqualTo("203.0.113.5");
    }
}
