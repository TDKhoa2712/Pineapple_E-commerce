package backend.pineapple_ecommerce.common.enums;

import backend.pineapple_ecommerce.modules.coupon.models.Coupon;
import java.math.BigDecimal;

public enum CouponType {
    PERCENTAGE {
        @Override
        public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
            BigDecimal discount = subtotal.multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null) {
                discount = discount.min(coupon.getMaxDiscountAmount());
            }
            return discount;
        }
    },
    FIXED_AMOUNT {
        @Override
        public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
            return coupon.getValue().min(subtotal);
        }
    };

    public abstract BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal);
}
