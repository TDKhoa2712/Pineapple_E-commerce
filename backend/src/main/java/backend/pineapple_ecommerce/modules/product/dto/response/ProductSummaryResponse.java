package backend.pineapple_ecommerce.modules.product.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class ProductSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private BigDecimal discountPrice;
    // FIX: thêm effectivePrice (min của price/discountPrice) cho FE hiển thị
    private BigDecimal effectivePrice;
    private String thumbnail;
    private Boolean isOrganic;
    private String status;
    private String statusReason;
    private Long categoryId;
    private String categoryName;
    private Integer totalStock;
    private Double averageRating;
    private Integer reviewCount;
    // FIX: thêm farm info để FE ProductCard hiển thị trang trại
    private Long farmId;
    private String farmName;
    // FIX: thêm unit (đơn vị tính: kg, bó, quả...)
    private String unit;
    // FIX: thêm soldCount để FE sort "Bán chạy nhất"
    private Integer soldCount;
}
