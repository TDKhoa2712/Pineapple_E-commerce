package backend.pineapple_ecommerce.modules.coupon.dto.response;

import backend.pineapple_ecommerce.common.enums.CouponType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private Long id;
    private String code;
    private CouponType type;
    private BigDecimal value;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private Integer totalLimit;
    private Integer usedCount;
    private Integer perUserLimit;
    private Boolean isActive;
    private Set<Long> applicableProductIds;
    private Set<Long> applicableCategoryIds;
    private Long createdById;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
