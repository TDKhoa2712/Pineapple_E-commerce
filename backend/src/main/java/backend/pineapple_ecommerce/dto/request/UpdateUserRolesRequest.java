package backend.pineapple_ecommerce.dto.request;

import backend.pineapple_ecommerce.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRolesRequest {

    /**
     * Tập hợp role mới cho user.
     * Sẽ REPLACE toàn bộ roles hiện tại (không phải append).
     * Phải có ít nhất 1 role.
     * Ví dụ: ["ROLE_USER", "ROLE_FARMER"]
     */
    @NotEmpty(message = "Phải có ít nhất một role")
    private Set<RoleName> roles;
}
