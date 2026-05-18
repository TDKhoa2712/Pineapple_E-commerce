package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body cho POST /api/v1/auth/resend-verification
 * Người dùng yêu cầu gửi lại OTP xác thực email.
 */
@Getter
@Setter
public class ResendVerificationRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
}