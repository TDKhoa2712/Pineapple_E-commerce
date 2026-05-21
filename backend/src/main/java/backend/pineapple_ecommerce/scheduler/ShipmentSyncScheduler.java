package backend.pineapple_ecommerce.scheduler;

import backend.pineapple_ecommerce.config.ShippingProperties;
import backend.pineapple_ecommerce.entity.Shipment;
import backend.pineapple_ecommerce.repository.ShipmentRepository;
import backend.pineapple_ecommerce.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scheduler đồng bộ trạng thái vận đơn định kỳ cho TẤT CẢ carriers.
 *
 * <p>Thay thế {@code GhnShipmentSyncScheduler} — hoạt động với mọi carrier
 * đã được đăng ký trong {@link backend.pineapple_ecommerce.service.carrier.ShippingProviderRouter}.
 *
 * <p>Khi thêm carrier mới, scheduler này tự động xử lý luôn — không cần sửa.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentSyncScheduler {

    private final ShippingService     shippingService;
    private final ShipmentRepository  shipmentRepository;
    private final ShippingProperties  shippingProperties;

    /**
     * Sync tất cả vận đơn đang active mỗi N phút (cấu hình từ ShippingProperties).
     * Nhóm theo carrier để dễ theo dõi log và giới hạn rate.
     */
    @Scheduled(
            fixedDelayString  = "#{${app.shipping.sync-interval-minutes:30} * 60 * 1000}",
            initialDelayString = "#{5 * 60 * 1000}"  // delay 5 phút khi start
    )
    public void syncActiveShipments() {
        List<Shipment> activeShipments = shipmentRepository.findAllActive();

        if (activeShipments.isEmpty()) {
            log.debug("No active shipments to sync");
            return;
        }

        // Nhóm theo carrier để theo dõi riêng
        Map<String, List<Shipment>> byCarrier = activeShipments.stream()
                .collect(Collectors.groupingBy(s -> s.getCarrierCode().name()));

        log.info("Starting shipment sync: {} active shipments across {} carriers: {}",
                activeShipments.size(), byCarrier.size(), byCarrier.keySet());

        int totalSynced = 0, totalFailed = 0;

        for (Shipment shipment : activeShipments) {
            try {
                shippingService.syncStatus(
                        shipment.getExternalOrderCode(),
                        shipment.getCarrierCode()
                );
                totalSynced++;

                // Delay giữa các request — tránh spam API của carrier
                long delay = shippingProperties.getSyncDelayMs();
                if (delay > 0) Thread.sleep(delay);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Shipment sync interrupted");
                break;
            } catch (Exception e) {
                totalFailed++;
                log.warn("Failed to sync shipment {} ({}): {}",
                        shipment.getExternalOrderCode(), shipment.getCarrierCode(), e.getMessage());
            }
        }

        log.info("Shipment sync complete: synced={}, failed={}/{} | By carrier: {}",
                totalSynced, totalFailed, activeShipments.size(),
                byCarrier.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue().size())
                        .collect(Collectors.joining(", ")));
    }
}