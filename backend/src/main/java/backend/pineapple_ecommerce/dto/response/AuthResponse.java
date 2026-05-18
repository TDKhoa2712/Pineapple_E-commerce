package backend.pineapple_ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    // ── JWT fields (null khi chưa verify email) ──────────────────────────
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long   expiresIn;

    // ── User info ─────────────────────────────────────────────────────────
    private Long        userId;
    private String      email;
    private String      fullName;
    private Set<String> roles;

    /**
     * true  → email đã xác thực, JWT được cấp bình thường.
     * false → email chưa xác thực, JWT bị giữ lại, FE cần hiển thị màn OTP.
     */
    private Boolean emailVerified;

    /**
     * Thông báo hướng dẫn (chỉ có khi emailVerified = false).
     * Ví dụ: "Vui lòng kiểm tra email để nhập mã xác thực."
     */
    private String message;
}
