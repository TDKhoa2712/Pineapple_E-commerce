package backend.pineapple_ecommerce.modules.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartItemResponse {
    private Long       id;
    private Long       productId;
    private String     productName;
    // FIX: thêm productSlug để FE có thể navigate đến trang sản phẩm
    private String     productSlug;
    private String     productThumbnail;
    // Backend field: unitPrice (FE types updated to match)
    private BigDecimal unitPrice;
    private Integer    quantity;
    private BigDecimal subtotal;
    // Backend field: availableStock (FE types updated to match)
    private Integer    availableStock;
    // FIX: thêm productStatus để FE biết sản phẩm còn active không
    private String     productStatus;
    // FIX: thêm productUnit để FE biết đơn vị tính của sản phẩm
    private String     productUnit;
}
