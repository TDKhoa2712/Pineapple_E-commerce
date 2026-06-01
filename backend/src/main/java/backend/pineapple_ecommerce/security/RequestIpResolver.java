package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.common.config.TrustedProxyProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RequestIpResolver {

    private static final AtomicReference<RequestIpResolver> INSTANCE = new AtomicReference<>();

    private final List<IpAddressMatcher> matchers;
    private final boolean trustAllProxies;

    public RequestIpResolver(TrustedProxyProperties properties) {
        this.matchers = new ArrayList<>();
        if (properties.getTrustedProxies() != null) {
            for (String ipOrCidr : properties.getTrustedProxies()) {
                this.matchers.add(new IpAddressMatcher(ipOrCidr));
            }
        }
        this.trustAllProxies = properties.isTrustAllProxies();
        
        // Register this instance as the static singleton
        setInstance(this);
    }

    public static void setInstance(RequestIpResolver resolver) {
        INSTANCE.set(resolver);
    }

    public static RequestIpResolver getInstance() {
        RequestIpResolver r = INSTANCE.get();
        if (r == null) {
            // Lazy initialization fallback (e.g. for unit tests)
            INSTANCE.compareAndSet(null, new RequestIpResolver(new TrustedProxyProperties()));
            return INSTANCE.get();
        }
        return r;
    }

    public boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        for (IpAddressMatcher matcher : matchers) {
            try {
                if (matcher.matches(ip)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // Invalid format, not a trusted proxy
            }
        }
        return false;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Normalize IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            remoteAddr = "127.0.0.1";
        }

        // If we don't trust all proxies and the remote address is not a trusted proxy,
        // ignore X-Forwarded-For completely.
        if (!trustAllProxies && !isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        // Retrieve standard X-Forwarded-For header variations
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            xForwardedFor = request.getHeader("X-FORWARDED-FOR");
        }
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            xForwardedFor = request.getHeader("x-forwarded-for");
        }

        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return remoteAddr;
        }

        // Traverse the X-Forwarded-For chain from right to left
        String[] ips = xForwardedFor.split(",");
        for (int i = ips.length - 1; i >= 0; i--) {
            String ip = ips[i].trim();
            if (ip.isBlank()) {
                continue;
            }
            // Normalize IPv6 localhost in headers too
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1";
            }
            if (!isTrustedProxy(ip)) {
                return ip;
            }
        }

        // Fallback to leftmost IP if all IPs in the list are trusted proxies
        if (ips.length > 0) {
            String leftmost = ips[0].trim();
            if (!leftmost.isBlank()) {
                return leftmost;
            }
        }

        return remoteAddr;
    }
}
