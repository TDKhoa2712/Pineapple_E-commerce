package backend.pineapple_ecommerce.modules.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO để admin cập nhật thông tin cá nhân của người dùng (fullName, phone).
 * Admin không thể update các field khác như email, status, roles qua endpoint này.
 */
@Getter
@Setter
public class AdminUpdateUserRequest {

    @Size(max = 100, message = "Họ tên không quá 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại không hợp lệ (định dạng: 0xxxxxxxxx)")
    private String phone;

}
