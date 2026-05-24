package backend.pineapple_ecommerce.modules.coupon.repository;

import backend.pineapple_ecommerce.modules.coupon.models.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    long countByCouponIdAndUserId(Long couponId, Long userId);

    List<CouponUsage> findAllByOrderId(Long orderId);

    List<CouponUsage> findAllByCouponId(Long couponId);

}
