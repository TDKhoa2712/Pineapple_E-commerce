package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.ConfirmPasswordResetRequest;
import backend.pineapple_ecommerce.dto.request.InitiatePasswordResetRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
    public ResponseEntity<ApiResponse<Void>> confirm(
            @Valid @RequestBody ConfirmPasswordResetRequest request) {

        passwordResetService.resetPassword(
                request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(
                ApiResponse.success(null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));
    }
}
