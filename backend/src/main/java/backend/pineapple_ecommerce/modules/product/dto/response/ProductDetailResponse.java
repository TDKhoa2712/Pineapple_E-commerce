package backend.pineapple_ecommerce.modules.product.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ProductDetailResponse implements Serializable {
    private static final long serialVersionUID = 1L;
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
