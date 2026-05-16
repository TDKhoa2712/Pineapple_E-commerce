package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Body cho API Admin từ chối farm */
@Getter
@Setter
public class RejectFarmRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String reason;
}
