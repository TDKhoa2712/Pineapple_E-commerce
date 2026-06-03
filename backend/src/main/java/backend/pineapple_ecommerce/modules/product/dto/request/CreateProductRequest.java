package backend.pineapple_ecommerce.modules.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên không quá 200 ký tự")
    private String name;

    // slug tự gen nếu để trống
    private String slug;

    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá không hợp lệ")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá khuyến mãi phải lớn hơn 0")
    private BigDecimal discountPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Trọng lượng phải > 0")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal weight;   // gram

    @DecimalMin(value = "0.0", inclusive = false, message = "Calories phải > 0")
    @Digits(integer = 4, fraction = 2, message = "Calories phải có tối đa 4 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal calories;

    @Size(max = 100)
    private String brand;

    @Size(max = 100)
    private String origin;

    @Size(max = 50)
    private String unit;

    private Boolean isOrganic = false;

    @NotBlank(message = "Thumbnail không được để trống")
    @Pattern(
            regexp = "^https://res\\.cloudinary\\.com/.*",
            message = "Thumbnail phải là URL Cloudinary hợp lệ"
    )
    private String thumbnail;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    private List<@Pattern(
            regexp = "^https://res\\.cloudinary\\.com/.*",
            message = "Ảnh phụ phải là URL Cloudinary hợp lệ"
    ) String> imageUrls;

}
