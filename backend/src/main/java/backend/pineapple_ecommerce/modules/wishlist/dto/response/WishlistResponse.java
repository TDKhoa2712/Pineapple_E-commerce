package backend.pineapple_ecommerce.modules.wishlist.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class WishlistResponse {
    private Long   id;
    private Long   productId;
    private String productName;
    private String productSlug;
    private String productThumbnail;
    private BigDecimal productPrice;
    private BigDecimal productDiscountPrice;
    private String     productStatus;
    private LocalDateTime createdAt;
}
