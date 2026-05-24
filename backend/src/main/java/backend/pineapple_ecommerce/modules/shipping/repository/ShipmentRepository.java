package backend.pineapple_ecommerce.modules.shipping.repository;

import backend.pineapple_ecommerce.common.enums.ShippingStatus;
import backend.pineapple_ecommerce.modules.shipping.models.Shipment;
import backend.pineapple_ecommerce.common.enums.CarrierCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho {@link Shipment} — provider-agnostic.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findByExternalOrderCode(String externalOrderCode);

    Optional<Shipment> findByClientOrderCode(String clientOrderCode);

    boolean existsByOrderId(Long orderId);

    /**
     * Lấy tất cả vận đơn đang active (chưa ở trạng thái terminal).
     */
    @Query("""
        SELECT s FROM Shipment s
        WHERE s.cancelledAt IS NULL
          AND s.externalOrderCode IS NOT NULL
          AND s.currentStatus NOT IN :excludedStatuses
        ORDER BY s.lastSyncAt ASC NULLS FIRST
    """)
    List<Shipment> findAllActive(@Param("excludedStatuses") List<ShippingStatus> excludedStatuses);

    /**
     * Lấy vận đơn active theo carrier cụ thể.
     */
    @Query("""
        SELECT s FROM Shipment s
        WHERE s.carrierCode = :carrierCode
          AND s.cancelledAt IS NULL
          AND s.externalOrderCode IS NOT NULL
          AND s.currentStatus NOT IN :excludedStatuses
        ORDER BY s.lastSyncAt ASC NULLS FIRST
    """)
    List<Shipment> findActiveByCarrier(
            @Param("carrierCode") CarrierCode carrierCode,
            @Param("excludedStatuses") List<ShippingStatus> excludedStatuses
    );

    /** Đếm vận đơn theo trạng thái và carrier */
    long countByCarrierCodeAndCurrentStatus(CarrierCode carrierCode, ShippingStatus status);
}