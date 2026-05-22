package backend.pineapple_ecommerce.modules.auth.controller;

import backend.pineapple_ecommerce.api.dto.auth.request.*;
import backend.pineapple_ecommerce.modules.auth.dto.request.*;
import backend.pineapple_ecommerce.modules.auth.service.AuthService;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.modules.auth.dto.response.AuthResponse;
import backend.pineapple_ecommerce.modules.user.dto.response.UserResponse;
import backend.pineapple_ecommerce.modules.auth.service.EmailVerificationService;
import backend.pineapple_ecommerce.modules.user.service.UserService;
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

    // ─────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────

    @Operation(
            summary = "Đăng ký tài khoản mới",
            description = "Tạo tài khoản LOCAL. Sau khi thành công, OTP sẽ được gửi tới email. " +
                    "Response KHÔNG chứa JWT — gọi /verify-email để nhận JWT."
    )
    @PostMapping("/register")
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
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        log.info("verify email run controller 1");
        // 1. Xác thực OTP, set emailVerified = true
        emailVerificationService.verifyEmail(request.getEmail(), request.getOtp());
        log.info("verify email run controller 2");

        // 2. Tự động login — cấp JWT ngay sau khi verify thành công
        //    Dùng lại buildAuthResponse nội bộ qua login không cần password
        //    (user đã được trust sau khi verify OTP)
        AuthResponse authResponse = authService.loginAfterVerification(request.getEmail());

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
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    // ─────────────────────────────────────────────
    // Refresh Token
    // ─────────────────────────────────────────────

    @Operation(summary = "Làm mới access token bằng refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token đã được làm mới"));
    }

    // ─────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────

    @Operation(summary = "Đăng xuất", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
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