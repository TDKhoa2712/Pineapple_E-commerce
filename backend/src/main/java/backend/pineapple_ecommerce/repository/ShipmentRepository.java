package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.Shipment;
import backend.pineapple_ecommerce.enums.CarrierCode;
import backend.pineapple_ecommerce.enums.ShippingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho {@link Shipment} — provider-agnostic.
 * Thay thế {@code GhnShipmentRepository}.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findByExternalOrderCode(String externalOrderCode);

    Optional<Shipment> findByClientOrderCode(String clientOrderCode);

    boolean existsByOrderId(Long orderId);

    /**
     * Lấy tất cả vận đơn đang active (chưa ở trạng thái terminal).
     * Dùng bởi scheduler để sync định kỳ.
     */
    @Query("""
        SELECT s FROM Shipment s
        WHERE s.cancelledAt IS NULL
          AND s.externalOrderCode IS NOT NULL
          AND s.currentStatus NOT IN (
              backend.pineapple_ecommerce.enums.ShippingStatus.DELIVERED,
              backend.pineapple_ecommerce.enums.ShippingStatus.RETURNED,
              backend.pineapple_ecommerce.enums.ShippingStatus.CANCELLED,
              backend.pineapple_ecommerce.enums.ShippingStatus.LOST
          )
        ORDER BY s.lastSyncAt ASC NULLS FIRST
    """)
    List<Shipment> findAllActive();

    /**
     * Lấy vận đơn active theo carrier cụ thể.
     * Dùng khi cần sync riêng từng carrier.
     */
    @Query("""
        SELECT s FROM Shipment s
        WHERE s.carrierCode = :carrierCode
          AND s.cancelledAt IS NULL
          AND s.externalOrderCode IS NOT NULL
          AND s.currentStatus NOT IN (
              backend.pineapple_ecommerce.enums.ShippingStatus.DELIVERED,
              backend.pineapple_ecommerce.enums.ShippingStatus.RETURNED,
              backend.pineapple_ecommerce.enums.ShippingStatus.CANCELLED,
              backend.pineapple_ecommerce.enums.ShippingStatus.LOST
          )
        ORDER BY s.lastSyncAt ASC NULLS FIRST
    """)
    List<Shipment> findActiveByCarrier(@Param("carrierCode") CarrierCode carrierCode);

    /** Đếm vận đơn theo trạng thái và carrier (dùng cho metrics / dashboard) */
    long countByCarrierCodeAndCurrentStatus(CarrierCode carrierCode, ShippingStatus status);
}