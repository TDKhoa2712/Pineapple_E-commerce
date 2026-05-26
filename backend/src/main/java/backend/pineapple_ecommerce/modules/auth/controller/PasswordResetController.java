package backend.pineapple_ecommerce.modules.auth.controller;

import backend.pineapple_ecommerce.modules.auth.dto.request.ConfirmPasswordResetRequest;
import backend.pineapple_ecommerce.modules.auth.dto.request.InitiatePasswordResetRequest;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.modules.auth.service.PasswordResetService;
import backend.pineapple_ecommerce.security.ratelimit.RateLimit;
import backend.pineapple_ecommerce.security.ratelimit.RateLimitType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints reset mật khẩu qua OTP email.
 *
 * <p>Luồng:
 * 1. POST /api/v1/auth/password-reset/initiate  — nhập email → gửi OTP
 * 2. POST /api/v1/auth/password-reset/confirm   — nhập OTP + mật khẩu mới
 *
 * <p>Cả hai endpoint là public (không cần JWT).
 * SecurityConfig cần thêm vào PUBLIC_POST:
 *   "/api/v1/auth/password-reset/**"
 */
@Tag(name = "Password Reset", description = "Đặt lại mật khẩu qua OTP email")
@RestController
@RequestMapping("/api/v1/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // ─────────────────────────────────────────────
    // STEP 1: Gửi OTP
    // ─────────────────────────────────────────────

    @Operation(summary = "Yêu cầu OTP đặt lại mật khẩu",
               description = "Gửi mã OTP 6 chữ số về email. Mã hết hạn sau 10 phút.")
    @PostMapping("/initiate")
    @RateLimit(maxRequests = 3, windowSeconds = 600, type = RateLimitType.IP_AND_EMAIL)
    public ResponseEntity<ApiResponse<Void>> initiate(
            @Valid @RequestBody InitiatePasswordResetRequest request) {

        passwordResetService.initiateReset(request.getEmail());
        // Luôn trả thành công — không tiết lộ email có tồn tại không
        return ResponseEntity.ok(
                ApiResponse.success(null, "Nếu email tồn tại, OTP đã được gửi. Vui lòng kiểm tra hộp thư."));
    }

    // ─────────────────────────────────────────────
    // STEP 2: Xác nhận OTP + đặt mật khẩu mới
    // ─────────────────────────────────────────────

    @Operation(summary = "Xác nhận OTP và đặt mật khẩu mới")
    @PostMapping("/confirm")
    @RateLimit(maxRequests = 5, windowSeconds = 60, type = RateLimitType.IP_AND_EMAIL)
    public ResponseEntity<ApiResponse<Void>> confirm(
            @Valid @RequestBody ConfirmPasswordResetRequest request) {

        passwordResetService.resetPassword(
                request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(
                ApiResponse.success(null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));
    }
}
