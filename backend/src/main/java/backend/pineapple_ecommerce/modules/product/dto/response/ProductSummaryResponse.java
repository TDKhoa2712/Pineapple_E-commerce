package backend.pineapple_ecommerce.modules.product.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;
    private String thumbnail;
    private Boolean isOrganic;
    private String status;
    private String categoryName;
    private Integer totalStock;
    private Double averageRating;
    // FIX: thêm reviewCount để ProductCard hiển thị số đánh giá
    private Integer reviewCount;
}
