package backend.pineapple_ecommerce.modules.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusReasonRequest {
    @NotBlank(message = "Ly do khong duoc de trong")
    @Size(max = 500, message = "Ly do toi da 500 ky tu")
    private String reason;
}
