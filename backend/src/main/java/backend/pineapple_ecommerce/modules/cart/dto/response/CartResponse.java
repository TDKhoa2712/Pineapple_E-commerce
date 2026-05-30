package backend.pineapple_ecommerce.modules.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartResponse {
    // FIX: expose "id" alias cho cartId để FE có thể dùng nhất quán
    // Backend internally dùng cartId, nhưng JSON response sẽ có cả "cartId" và "id"
    private Long cartId;

    // "id" alias để FE dùng response.data.id
    @JsonProperty("id")
    public Long getId() {
        return cartId;
    }

    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalAmount;
}
