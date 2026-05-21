package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.GhnShipment;
import backend.pineapple_ecommerce.enums.GhnShippingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GhnShipmentRepository extends JpaRepository<GhnShipment, Long> {

    Optional<GhnShipment> findByOrderId(Long orderId);

    Optional<GhnShipment> findByGhnOrderCode(String ghnOrderCode);

    Optional<GhnShipment> findByClientOrderCode(String clientOrderCode);

    boolean existsByOrderId(Long orderId);

    /**
     * Lấy tất cả vận đơn đang active (chưa hủy + chưa terminal).
     * Dùng bởi scheduler để sync định kỳ.
     *
     * <p>Terminal statuses: DELIVERED, RETURNED, CANCEL
     */
    @Query("""
        SELECT s FROM GhnShipment s
        WHERE s.cancelledAt IS NULL
          AND s.ghnOrderCode IS NOT NULL
          AND s.currentStatus NOT IN (
              backend.pineapple_ecommerce.enums.GhnShippingStatus.DELIVERED,
              backend.pineapple_ecommerce.enums.GhnShippingStatus.RETURNED,
              backend.pineapple_ecommerce.enums.GhnShippingStatus.CANCEL,
              backend.pineapple_ecommerce.enums.GhnShippingStatus.LOST
          )
        ORDER BY s.lastSyncAt ASC
    """)
    List<GhnShipment> findAllActive();
}
