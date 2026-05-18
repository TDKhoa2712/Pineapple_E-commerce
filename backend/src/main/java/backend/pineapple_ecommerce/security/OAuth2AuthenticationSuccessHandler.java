package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.config.CorsProperties;
import backend.pineapple_ecommerce.entity.RefreshToken;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
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

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository      userRepository;
    private final CorsProperties      corsProperties;

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
        // Lấy email từ OAuth2User
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = extractEmail(oAuth2User);

        // Load User entity để tạo UserDetails cho JwtService
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new IllegalStateException(
                        "User không tồn tại sau OAuth2 success: " + email));

        // Tạo tokens — dùng chung JwtService + RefreshTokenService với LOCAL flow
        org.springframework.security.core.userdetails.UserDetails userDetails =
                UserDetailsServiceImpl.buildUserDetails(user);

        String accessToken  = jwtService.generateAccessToken(userDetails);
        RefreshToken refresh = refreshTokenService.createRefreshToken(user);

        log.info("OAuth2 login success: {} → tạo JWT thành công", email);

        // Xây dựng redirect URL về FE
        String frontendUrl = resolveFrontendUrl();
        return UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken",  accessToken)
                .queryParam("refreshToken", refresh.getToken())
                .queryParam("expiresIn",    jwtService.getAccessTokenExpirationMs() / 1000)
                .queryParam("tokenType",    "Bearer")
                .build().toUriString();
    }

    /**
     * Trích xuất email từ OAuth2User — Google và Facebook đều có "email" attribute.
     * Fallback: dùng getName() nếu email attribute không tìm thấy.
     */
    private String extractEmail(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Object email = attributes.get("email");
        if (email instanceof String s && !s.isBlank()) {
            return s;
        }
        // Fallback không nên xảy ra vì CustomOAuth2UserService đã validate email
        throw new IllegalStateException("Không tìm thấy email trong OAuth2User attributes");
    }

    /**
     * Lấy frontend URL từ CorsProperties — tránh hardcode.
     * Nếu có nhiều allowed origins thì lấy cái đầu tiên (thường là FE URL).
     */
    private String resolveFrontendUrl() {
        return corsProperties.getAllowedOrigins().stream()
                .filter(origin -> !origin.equals("*"))
                .findFirst()
                .orElse("http://localhost:3000");
    }
}
