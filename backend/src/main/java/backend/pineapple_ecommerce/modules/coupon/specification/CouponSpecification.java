package backend.pineapple_ecommerce.modules.coupon.specification;

import backend.pineapple_ecommerce.common.enums.CouponType;
import backend.pineapple_ecommerce.modules.coupon.models.Coupon;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class CouponSpecification {

    private CouponSpecification() {}

    public static Specification<Coupon> isActive(Boolean active) {
        return (root, query, cb) ->
                active == null ? null : cb.equal(root.get("isActive"), active);
    }

    public static Specification<Coupon> isExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) return null;
            LocalDateTime now = LocalDateTime.now();
            if (expired) {
                return cb.lessThan(root.get("expiryDate"), now);
            } else {
                return cb.greaterThanOrEqualTo(root.get("expiryDate"), now);
            }
        };
    }

    public static Specification<Coupon> hasType(CouponType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }
}
