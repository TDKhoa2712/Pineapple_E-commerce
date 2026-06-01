package backend.pineapple_ecommerce.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Body cho POST /api/v1/auth/password-reset/initiate
 * Gửi OTP về email.
 */
@Getter
@Setter
public class InitiatePasswordResetRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}
