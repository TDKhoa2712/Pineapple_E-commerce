package backend.pineapple_ecommerce.modules.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class UpdateProductRequest {

    @Size(max = 200)
    private String name;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal discountPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Trọng lượng phải > 0")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal weight;   // gram
    @DecimalMin(value = "0.0", inclusive = false, message = "Calories phải > 0")
    @Digits(integer = 4, fraction = 2, message = "Calories phải có tối đa 4 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal calories;
    private String brand;
    private String origin;
    @Size(max = 50)
    private String unit;
    private Boolean isOrganic;
    private String thumbnail;
    private Long categoryId;
    private String status;          // ACTIVE | INACTIVE | OUT_OF_STOCK
    private List<String> imageUrls;
}
