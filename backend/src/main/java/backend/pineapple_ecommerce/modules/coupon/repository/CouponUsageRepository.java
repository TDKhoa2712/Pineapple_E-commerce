package backend.pineapple_ecommerce.modules.coupon.repository;

import backend.pineapple_ecommerce.modules.coupon.models.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    long countByCouponIdAndUserId(Long couponId, Long userId);

    List<CouponUsage> findAllByOrderId(Long orderId);

    @Query("""
        SELECT cu FROM CouponUsage cu
        LEFT JOIN FETCH cu.coupon
        LEFT JOIN FETCH cu.user
        WHERE cu.coupon.id = :couponId
    """)
    List<CouponUsage> findAllByCouponId(@Param("couponId") Long couponId);

}
