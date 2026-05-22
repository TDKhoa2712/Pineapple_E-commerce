package backend.pineapple_ecommerce.modules.cart.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartItemResponse {
    private Long       id;
    private Long       productId;
    private String     productName;
    private String     productThumbnail;
    private BigDecimal unitPrice;
    private Integer    quantity;
    private BigDecimal subtotal;
    private Integer    availableStock;
}
