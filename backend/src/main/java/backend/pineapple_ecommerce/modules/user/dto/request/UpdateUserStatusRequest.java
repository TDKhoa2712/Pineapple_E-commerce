package backend.pineapple_ecommerce.modules.user.dto.request;

import backend.pineapple_ecommerce.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private UserStatus status;

    private String reason; // Lý do khoá/mở khoá (ghi log)
}
