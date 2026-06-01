package backend.pineapple_ecommerce.modules.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CouponPreviewRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    private String couponCode;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    private BigDecimal cartTotal;
}
