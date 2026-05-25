package backend.pineapple_ecommerce.modules.coupon.repository;

import backend.pineapple_ecommerce.modules.coupon.models.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code)")
    Optional<Coupon> findByCodeIgnoreCaseWithLock(@Param("code") String code);

    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :id AND c.usedCount < c.totalLimit")
    int incrementUsedCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount - 1 WHERE c.id = :id AND c.usedCount > 0")
    int decrementUsedCount(@Param("id") Long id);
}
