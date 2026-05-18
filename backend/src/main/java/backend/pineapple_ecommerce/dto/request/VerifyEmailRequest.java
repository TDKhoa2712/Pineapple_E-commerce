package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body cho POST /api/v1/auth/verify-email
 * Người dùng nhập email + mã OTP 6 chữ số nhận qua email.
 */
@Getter
@Setter
public class VerifyEmailRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "\\d{6}", message = "Mã OTP phải là 6 chữ số")
    private String otp;
}