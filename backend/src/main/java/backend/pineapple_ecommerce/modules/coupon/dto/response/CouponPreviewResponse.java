package backend.pineapple_ecommerce.modules.coupon.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponPreviewResponse {
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal newTotal;
}
