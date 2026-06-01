package backend.pineapple_ecommerce.modules.auth.controller;

import backend.pineapple_ecommerce.modules.auth.dto.request.*;
import backend.pineapple_ecommerce.modules.auth.service.AuthService;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.modules.auth.dto.response.AuthResponse;
import backend.pineapple_ecommerce.modules.user.dto.response.UserResponse;
import backend.pineapple_ecommerce.modules.auth.service.EmailVerificationService;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import backend.pineapple_ecommerce.security.ratelimit.RateLimit;
import backend.pineapple_ecommerce.security.ratelimit.RateLimitType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import backend.pineapple_ecommerce.common.config.JwtProperties;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Authentication endpoints.
 */
@Slf4j
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, xác thực email, refresh token, logout")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final UserService userService;
    private final CacheManager cacheManager;
    private final JwtProperties jwtProperties;

    // FIX: Đọc từ config — false trong dev (HTTP), true trong prod (HTTPS)
    // application-dev.yml: app.cookie.secure=false
    // application-prod.yml: app.cookie.secure=true
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    // ─────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────

    @Operation(
            summary = "Đăng ký tài khoản mới",
            description = "Tạo tài khoản LOCAL. Sau khi thành công, OTP sẽ được gửi tới email. " +
                    "Response KHÔNG chứa JWT — gọi /verify-email để nhận JWT."
    )
    @PostMapping("/register")
    @RateLimit(maxRequests = 5, windowSeconds = 60, type = RateLimitType.IP_AND_EMAIL)
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đăng ký thành công. Vui lòng xác thực email."));
    }

    // ─────────────────────────────────────────────
    // Verify Email (NEW)
    // ─────────────────────────────────────────────

    @Operation(
            summary = "Xác thực email bằng OTP",
            description = "Người dùng nhập OTP nhận qua email. Sau khi xác thực thành công, " +
                    "response sẽ trả JWT đầy đủ (accessToken + refreshToken)."
    )
    @PostMapping("/verify-email")
    @RateLimit(maxRequests = 5, windowSeconds = 60, type = RateLimitType.IP_AND_EMAIL)
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletResponse servletResponse) {
        log.info("verify email run controller 1");
        // 1. Xác thực OTP, set emailVerified = true
        emailVerificationService.verifyEmail(request.getEmail(), request.getOtp());
        log.info("verify email run controller 2");

        // 2. Tự động login — cấp JWT ngay sau khi verify thành công
        //    Dùng lại buildAuthResponse nội bộ qua login không cần password
        //    (user đã được trust sau khi verify OTP)
        AuthResponse authResponse = authService.loginAfterVerification(request.getEmail());

        if (Boolean.TRUE.equals(authResponse.getEmailVerified())) {
            setAccessTokenCookie(servletResponse, authResponse.getAccessToken());
            setRefreshTokenCookie(servletResponse, authResponse.getRefreshToken());
            authResponse.setAccessToken(null);
            authResponse.setRefreshToken(null);
        }

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Email xác thực thành công!"));
    }

    // ─────────────────────────────────────────────
    // Resend Verification OTP (NEW)
    // ─────────────────────────────────────────────

    @Operation(
            summary = "Gửi lại OTP xác thực email",
            description = "Gửi lại OTP cho email chưa xác thực. " +
                    "Rate limit: tối đa 3 lần trong 10 phút."
    )
    @PostMapping("/resend-verification")
    @RateLimit(maxRequests = 3, windowSeconds = 600, type = RateLimitType.IP_AND_EMAIL)
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {

        emailVerificationService.resendVerificationOtp(request.getEmail());
        return ResponseEntity.ok(
                ApiResponse.success(null, "Mã OTP mới đã được gửi tới email của bạn."));
    }

    // ─────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────

    @Operation(
            summary = "Đăng nhập",
            description = "LOCAL user phải xác thực email trước khi đăng nhập được."
    )
    @PostMapping("/login")
    @RateLimit(maxRequests = 5, windowSeconds = 60, type = RateLimitType.IP_AND_EMAIL)
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse servletResponse) {

        AuthResponse response = authService.login(request);
        if (Boolean.TRUE.equals(response.getEmailVerified())) {
            setAccessTokenCookie(servletResponse, response.getAccessToken());
            setRefreshTokenCookie(servletResponse, response.getRefreshToken());
            response.setAccessToken(null);
            response.setRefreshToken(null);
        }
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    // ─────────────────────────────────────────────
    // OAuth2 Exchange
    // ─────────────────────────────────────────────

    @Operation(
            summary = "Trao đổi mã code OAuth2 lấy token",
            description = "Nhận mã tạm thời từ URL callback và trả về Access Token trong body và Refresh Token trong HttpOnly Cookie."
    )
    @PostMapping("/oauth2/exchange")
    public ResponseEntity<ApiResponse<AuthResponse>> exchangeOAuth2Code(
            @Valid @RequestBody OAuth2ExchangeRequest request,
            HttpServletResponse servletResponse) {

        Cache cache = cacheManager.getCache("oauth2_codes");
        if (cache == null) {
            throw new BusinessException("Hệ thống cache không hoạt động");
        }

        AuthResponse authResponse = cache.get(request.getCode(), AuthResponse.class);
        if (authResponse == null) {
            throw new BusinessException("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }

        // Xoá code khỏi cache để đảm bảo sử dụng 1 lần duy nhất (Single-Use)
        cache.evict(request.getCode());

        // Lấy tokens từ response và set vào HttpOnly Cookies
        setRefreshTokenCookie(servletResponse, authResponse.getRefreshToken());
        setAccessTokenCookie(servletResponse, authResponse.getAccessToken());

        // Xoá tokens khỏi response body để tăng tính bảo mật
        authResponse.setRefreshToken(null);
        authResponse.setAccessToken(null);

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Đăng nhập OAuth2 thành công!"));
    }

    // ─────────────────────────────────────────────
    // Refresh Token
    // ─────────────────────────────────────────────

    @Operation(summary = "Làm mới access token bằng refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String token = extractRefreshToken(request, servletRequest);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("Refresh token không được để trống");
        }

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest(token));

        // Thiết lập Cookie Refresh Token và Access Token mới
        setRefreshTokenCookie(servletResponse, response.getRefreshToken());
        setAccessTokenCookie(servletResponse, response.getAccessToken());
        response.setRefreshToken(null);
        response.setAccessToken(null);

        return ResponseEntity.ok(ApiResponse.success(response, "Token đã được làm mới"));
    }

    // ─────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────

    @Operation(summary = "Đăng xuất", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String token = extractRefreshToken(request, servletRequest);
        if (StringUtils.hasText(token)) {
            authService.logout(token);
        }

        // Xoá cookie refresh token và access token
        setRefreshTokenCookie(servletResponse, null);
        setAccessTokenCookie(servletResponse, null);

        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    // ─────────────────────────────────────────────
    // Cookie Helpers (NEW)
    // ─────────────────────────────────────────────

    private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        long maxAge = accessToken == null ? 0 : jwtProperties.getAccessTokenExpirationMs() / 1000;
        ResponseCookie cookie = ResponseCookie.from("access_token", accessToken == null ? "" : accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAge = refreshToken == null ? 0 : jwtProperties.getRefreshTokenExpirationMs() / 1000;
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken == null ? "" : refreshToken)
                .httpOnly(true)
                // FIX: Đọc từ config — false trong dev (HTTP localhost), true trong prod (HTTPS)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshToken(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            return request.getRefreshToken();
        }
        if (servletRequest.getCookies() != null) {
            return Arrays.stream(servletRequest.getCookies())
                    .filter(c -> "refresh_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // Me
    // ─────────────────────────────────────────────

    @Operation(summary = "Thông tin user hiện tại", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(userService.getUserByEmail(userDetails.getUsername())));

    }
}