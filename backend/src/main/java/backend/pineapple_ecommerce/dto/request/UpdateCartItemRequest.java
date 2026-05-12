package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCartItemRequest {

    @NotNull
    @Min(value = 0, message = "Số lượng phải >= 0 (0 = xoá)")
    private Integer quantity;
}
