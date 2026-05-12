package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;   // giá sau giảm
    private String thumbnail;
    private Boolean isOrganic;
    private String status;
    private String categoryName;
    private Integer totalStock;       // tổng tồn kho
    private Double averageRating;
}
