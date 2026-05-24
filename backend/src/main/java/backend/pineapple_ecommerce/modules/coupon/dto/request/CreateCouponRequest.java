package backend.pineapple_ecommerce.modules.coupon.dto.request;

import backend.pineapple_ecommerce.common.enums.CouponType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class CreateCouponRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(max = 50, message = "Mã không được quá 50 ký tự")
    private String code;

    @NotNull(message = "Loại giảm giá không được để trống")
    private CouponType type;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal value;

    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Giá trị đơn hàng tối thiểu không được để trống")
    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private LocalDateTime expiryDate;

    @NotNull(message = "Giới hạn số lần sử dụng không được để trống")
    @Min(value = 1, message = "Giới hạn sử dụng phải từ 1 trở lên")
    private Integer totalLimit;

    @NotNull(message = "Giới hạn mỗi user không được để trống")
    @Min(value = 1, message = "Giới hạn mỗi user phải từ 1 trở lên")
    private Integer perUserLimit = 1;

    private Set<Long> applicableProductIds;
    private Set<Long> applicableCategoryIds;
}
