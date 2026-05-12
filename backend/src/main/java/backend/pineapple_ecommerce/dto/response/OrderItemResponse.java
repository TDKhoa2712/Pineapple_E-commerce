package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productThumbnail;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
