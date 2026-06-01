package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.common.config.CorsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý khi OAuth2 authentication thất bại (user từ chối cấp quyền,
 * provider lỗi, email không lấy được, v.v.).
 *
 * Redirect về FE kèm error message trong query param để FE hiển thị thông báo:
 * https://frontend.com/login?error=oauth2&message=Không+lấy+được+email...
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final CorsProperties corsProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 authentication failure: {}", exception.getMessage());

        String errorMessage = exception.getLocalizedMessage() != null
                ? exception.getLocalizedMessage()
                : "Đăng nhập mạng xã hội thất bại. Vui lòng thử lại.";

        String frontendUrl = corsProperties.getAllowedOrigins().stream()
                .filter(origin -> !origin.contains("*"))
                .findFirst()
                .orElse("http://localhost:3000");

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                .queryParam("error", "oauth2")
                .queryParam("message", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
