package backend.pineapple_ecommerce.modules.coupon.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsageResponse {
    private Long id;
    private Long couponId;
    private String couponCode;
    private Long userId;
    private String userEmail;
    private Long orderId;
    private BigDecimal discountApplied;
    private LocalDateTime usedAt;
}
