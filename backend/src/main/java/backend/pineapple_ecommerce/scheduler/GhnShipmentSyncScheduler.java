package backend.pineapple_ecommerce.scheduler;

import backend.pineapple_ecommerce.entity.GhnShipment;
import backend.pineapple_ecommerce.enums.GhnShippingStatus;
import backend.pineapple_ecommerce.repository.GhnShipmentRepository;
import backend.pineapple_ecommerce.service.GhnShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler đồng bộ trạng thái vận đơn GHN định kỳ.
 *
 * <p>Tại sao cần scheduler ngoài webhook?
 * - Webhook có thể bị miss do mạng, downtime server
 * - Polling đảm bảo trạng thái luôn được cập nhật dù webhook lỗi
 *
 * <p>Chạy mỗi 30 phút, chỉ sync các vận đơn đang active
 * (không sync vận đơn đã giao/hủy/hoàn để tiết kiệm API quota GHN).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GhnShipmentSyncScheduler {

    private final GhnShippingService shippingService;
    private final GhnShipmentRepository ghnShipmentRepository;

    /**
     * Sync tất cả vận đơn đang active mỗi 30 phút.
     * Chỉ sync các vận đơn:
     * - Chưa bị hủy (cancelledAt IS NULL)
     * - Chưa ở trạng thái terminal (delivered/returned/cancel)
     * - lastSyncAt > 25 phút trước (tránh sync quá dày)
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 5 * 60 * 1000) // 30 phút, delay 5 phút khi start
    public void syncActiveShipments() {
        List<GhnShipment> activeShipments = ghnShipmentRepository.findAllActive();

        if (activeShipments.isEmpty()) {
            log.debug("No active GHN shipments to sync");
            return;
        }

        log.info("Starting GHN shipment sync: {} active shipments", activeShipments.size());
        int synced = 0, failed = 0;

        for (GhnShipment shipment : activeShipments) {
            try {
                shippingService.syncStatusFromGhn(shipment.getGhnOrderCode());
                synced++;
                // Tránh spam GHN API — delay nhỏ giữa các request
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to sync GHN shipment {}: {}", shipment.getGhnOrderCode(), e.getMessage());
            }
        }

        log.info("GHN sync complete: synced={}, failed={}/{}", synced, failed, activeShipments.size());
    }
}
