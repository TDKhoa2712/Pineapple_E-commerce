package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;
    private BigDecimal weight;
    private BigDecimal calories;
    private String brand;
    private String origin;
    private Boolean isOrganic;
    private String thumbnail;
    private String status;

    private Long categoryId;
    private String categoryName;

    private List<String> imageUrls;
    private Integer totalStock;
    private Double averageRating;
    private Integer reviewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
