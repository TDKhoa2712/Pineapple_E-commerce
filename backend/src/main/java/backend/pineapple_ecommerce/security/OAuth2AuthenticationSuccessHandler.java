package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.config.CorsProperties;
import backend.pineapple_ecommerce.dto.response.AuthResponse;
import backend.pineapple_ecommerce.entity.RefreshToken;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.OAuth2Service;
import backend.pineapple_ecommerce.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Xử lý sau khi OAuth2 authentication thành công:
 * 1. Lấy email từ OAuth2User principal
 * 2. Load User entity từ DB
 * 3. Tạo JWT access token + refresh token (dùng chung infrastructure với LOCAL login)
 * 4. Redirect về FE kèm token trong query params
 *
 * Về bảo mật redirect:
 * - Chỉ redirect về domain có trong CorsProperties.allowedOrigins
 * - Không hardcode URL → cấu hình qua env variable FRONTEND_URL
 *
 * Token delivery strategy — query param vs cookie:
 * - Query param: đơn giản, FE dễ xử lý, nhưng token lộ trong browser history/log
 * - HttpOnly cookie: an toàn hơn, không bị XSS đọc, nhưng phức tạp hơn với CORS
 * - Hiện tại: dùng query param cho simplicity, có thể chuyển sang cookie sau
 *
 * FE nhận được redirect:
 * https://frontend.com/oauth2/callback?accessToken=xxx&refreshToken=yyy&expiresIn=3600
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // Session attribute key — FE gửi redirect_uri trong initial request
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";

    private final OAuth2Service oauth2Service;
    private final UserRepository userRepository;
    private final CorsProperties corsProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response đã committed, không thể redirect về {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        // Lấy CustomUserDetails — nhất quán type, không cần cast phức tạp
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        String email = principal.getEmail();

        // Load User entity → tạo AuthResponse qua OAuth2Service
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new IllegalStateException(
                        "User không tồn tại sau OAuth2 success: " + email));

        AuthResponse authResponse = oauth2Service.buildAuthResponse(user);
        log.info("OAuth2 login success: {} → JWT created", email);

        // Xác định redirect target: ưu tiên redirect_uri từ FE (nếu hợp lệ)
        String redirectUri = resolveRedirectUri(request);
        String frontendCallbackUrl = StringUtils.hasText(redirectUri)
                ? redirectUri
                : resolveFrontendUrl() + "/oauth2/callback";

        return UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("accessToken",  authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .queryParam("expiresIn",    authResponse.getExpiresIn())
                .queryParam("tokenType",    "Bearer")
                .build().toUriString();
    }

    /**
     * Đọc redirect_uri mà FE gửi kèm trong session trước khi bắt đầu OAuth2 flow.
     * FE lưu vào session qua: /oauth2/authorization/google?redirect_uri=...
     * (Cần FE custom khi gọi initiate URL — xem tài liệu FE integration bên dưới)
     *
     * Bảo mật: validate domain trước khi dùng — chống Open Redirect attack.
     */
    private String resolveRedirectUri(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;

        Object redirectUri = session.getAttribute(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (!(redirectUri instanceof String uri) || !StringUtils.hasText(uri)) return null;

        // Cleanup session
        session.removeAttribute(REDIRECT_URI_PARAM_COOKIE_NAME);

        return isAllowedRedirectUri(uri) ? uri : null;
    }

    /**
     * Validate redirectUri chống Open Redirect.
     * Chỉ cho phép redirect về domain có trong CorsProperties.allowedOrigins.
     */
    private boolean isAllowedRedirectUri(String uri) {
        try {
            URI targetUri = URI.create(uri);
            String targetHost = targetUri.getHost();
            List<String> allowedOrigins = corsProperties.getAllowedOrigins();

            boolean isAllowed = allowedOrigins.stream().anyMatch(origin -> {
                if (origin.equals("*")) return true;
                try {
                    URI allowedUri = URI.create(origin);
                    return allowedUri.getHost() != null
                            && allowedUri.getHost().equalsIgnoreCase(targetHost);
                } catch (Exception e) {
                    return false;
                }
            });

            if (!isAllowed) {
                log.warn("Blocked redirect to unauthorized URI: {}", uri);
            }
            return isAllowed;
        } catch (Exception e) {
            log.warn("Invalid redirect URI: {}", uri);
            return false;
        }
    }

    private String resolveFrontendUrl() {
        return corsProperties.getAllowedOrigins().stream()
                .filter(origin -> !origin.equals("*"))
                .findFirst()
                .orElse("http://localhost:3000");
    }
}
