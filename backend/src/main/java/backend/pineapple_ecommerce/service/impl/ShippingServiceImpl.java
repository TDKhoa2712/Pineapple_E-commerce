package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.config.ShippingProperties;
import backend.pineapple_ecommerce.dto.request.CalculateShippingFeeRequest;
import backend.pineapple_ecommerce.dto.response.ShippingFeeResponse;
import backend.pineapple_ecommerce.dto.response.ShippingTrackingResponse;
import backend.pineapple_ecommerce.entity.Order;
import backend.pineapple_ecommerce.entity.OrderItem;
import backend.pineapple_ecommerce.entity.Shipment;
import backend.pineapple_ecommerce.enums.CarrierCode;
import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.enums.PaymentMethod;
import backend.pineapple_ecommerce.enums.ShippingStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.repository.OrderRepository;
import backend.pineapple_ecommerce.repository.ShipmentRepository;
import backend.pineapple_ecommerce.service.ShippingService;
import backend.pineapple_ecommerce.service.carrier.CarrierAddressMetadataHelper;
import backend.pineapple_ecommerce.service.carrier.ShippingCarrierClient;
import backend.pineapple_ecommerce.service.carrier.ShippingCarrierClient.*;
import backend.pineapple_ecommerce.service.carrier.ShippingProviderRouter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation ShippingService — điều phối mọi yêu cầu giao hàng
 * thông qua {@link ShippingProviderRouter}.
 *
 * <p>Class này KHÔNG chứa logic GHN cụ thể. Tất cả chi tiết carrier
 * nằm trong các implementation của {@link ShippingCarrierClient}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShippingProviderRouter    router;
    private final ShipmentRepository        shipmentRepository;
    private final OrderRepository           orderRepository;
    private final CarrierAddressMetadataHelper metadataHelper;
    private final ShippingProperties        shippingProperties;
    private final ObjectMapper              objectMapper;

    // ─────────────────────────────────────────────
    // Calculate Fee
    // ─────────────────────────────────────────────

    @Override
    public ShippingFeeResponse calculateFee(CalculateShippingFeeRequest request, CarrierCode carrierCode) {
        CarrierCode effectiveCarrier = resolveCarrier(carrierCode);
        ShippingCarrierClient client = router.getClient(effectiveCarrier);

        log.info("Calculating fee via {}: district={}, ward={}, weight={}",
                effectiveCarrier, request.getToDistrictId(), request.getToWardCode(), request.getWeight());

        FeeRequest feeReq = new FeeRequest(
                null,
                String.valueOf(request.getToDistrictId()),
                request.getToWardCode(),
                request.getWeight(),
                request.getLength() != null ? request.getLength() : 20,
                request.getWidth()  != null ? request.getWidth()  : 20,
                request.getHeight() != null ? request.getHeight() : 10,
                request.getInsuranceValue() != null ? request.getInsuranceValue() : 0,
                request.getServiceTypeId() != null ? String.valueOf(request.getServiceTypeId()) : null,
                request.getCoupon()
        );

        FeeResult result = client.calculateFee(feeReq);

        return ShippingFeeResponse.builder()
                .carrierCode(effectiveCarrier)
                .carrierName(effectiveCarrier.getDisplayName())
                .serviceFee(result.serviceFee())
                .insuranceFee(result.insuranceFee())
                .totalFee(result.totalFee())
                .codFee(result.codFee())
                .couponDiscount(result.couponDiscount())
                .expectedDeliveryTime(result.expectedDeliveryTime())
                .serviceId(result.serviceId())
                .build();
    }

    // ─────────────────────────────────────────────
    // Create Shipment
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ShippingTrackingResponse createShipment(Long orderId, CarrierCode carrierCode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new BusinessException(
                    "Chỉ tạo vận đơn khi đơn hàng ở PROCESSING, hiện tại: " + order.getStatus());
        }

        // Kiểm tra vận đơn đã tồn tại
        shipmentRepository.findByOrderId(orderId).ifPresent(existing -> {
            if (existing.isActive()) {
                throw new BusinessException(
                        "Đơn #" + orderId + " đã có vận đơn " + existing.getCarrierCode() +
                                ": " + existing.getExternalOrderCode());
            }
        });

        CarrierCode effectiveCarrier = resolveCarrier(carrierCode);
        ShippingCarrierClient client = router.getClient(effectiveCarrier);

        CreateShipmentRequest shipReq = buildCreateShipmentRequest(order, effectiveCarrier);
        CreateShipmentResult result = client.createShipment(shipReq);

        log.info("Shipment created via {}: externalCode={}, fee={}",
                effectiveCarrier, result.externalOrderCode(), result.totalFee());

        // Lưu DB
        Shipment shipment = Shipment.builder()
                .order(order)
                .carrierCode(effectiveCarrier)
                .externalOrderCode(result.externalOrderCode())
                .clientOrderCode(String.valueOf(orderId))
                .currentStatus(ShippingStatus.PENDING_PICKUP)
                .rawStatus("ready_to_pick")
                .shippingFee(result.shippingFee())
                .totalFee(result.totalFee())
                .expectedDeliveryTime(parseDateTime(result.expectedDeliveryTime()))
                .createdOnCarrierAt(LocalDateTime.now())
                .lastSyncAt(LocalDateTime.now())
                .carrierMetadata(result.carrierMetadataJson())
                .build();

        shipmentRepository.save(shipment);

        // Cập nhật shippingFee vào Order nếu chưa có
        if (order.getShippingFee().compareTo(BigDecimal.ZERO) == 0) {
            order.setShippingFee(result.shippingFee() != null ? result.shippingFee() : BigDecimal.ZERO);
            order.setTotalAmount(order.getSubtotal().add(order.getShippingFee()));
            orderRepository.save(order);
        }

        return toTrackingResponse(shipment);
    }

    // ─────────────────────────────────────────────
    // Get Tracking
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ShippingTrackingResponse getTracking(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        // Authorize — null = admin bypass
        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xem thông tin này");
        }

        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Đơn hàng #" + orderId + " chưa có vận đơn giao hàng"));

        return toTrackingResponse(shipment);
    }

    // ─────────────────────────────────────────────
    // Sync Status (webhook / scheduler)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void syncStatus(String externalOrderCode, CarrierCode carrierCode) {
        Shipment shipment = shipmentRepository.findByExternalOrderCode(externalOrderCode)
                .orElseGet(() -> {
                    log.warn("Received webhook for unknown shipment: {} ({})", externalOrderCode, carrierCode);
                    return null;
                });

        if (shipment == null) return;

        try {
            ShippingCarrierClient client = router.getClient(carrierCode);
            TrackingResult tracking = client.getTracking(externalOrderCode);

            log.info("Syncing status {}: {} → {}",
                    externalOrderCode, shipment.getCurrentStatus(), tracking.normalizedStatus());

            shipment.setCurrentStatus(tracking.normalizedStatus());
            shipment.setRawStatus(tracking.rawStatus());
            shipment.setFailReason(tracking.failReason());
            shipment.setLastSyncAt(LocalDateTime.now());

            // Lưu lịch sử tracking dạng JSON
            if (tracking.statusHistory() != null) {
                try {
                    shipment.setStatusLog(objectMapper.writeValueAsString(tracking.statusHistory()));
                } catch (Exception ignored) {}
            }

            // Cập nhật OrderStatus nội bộ
            OrderStatus mappedOrderStatus = tracking.normalizedStatus().toOrderStatus();
            if (mappedOrderStatus != null) {
                Order order = shipment.getOrder();

//                if (order.getStatus() != mappedOrderStatus && order.getStatus().canTransitionTo(mappedOrderStatus)) {
                if (order.getStatus() != mappedOrderStatus) {
                    order.setStatus(mappedOrderStatus);
                    orderRepository.save(order);
                    log.info("Updated order #{} status to {}", order.getId(), mappedOrderStatus);
                }
            }

            shipmentRepository.save(shipment);

        } catch (Exception e) {
            log.error("Failed to sync status for {} ({}): {}", externalOrderCode, carrierCode, e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    // Cancel Shipment
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void cancelShipment(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);

        if (shipment == null || !shipment.isActive()) {
            log.info("No active shipment for order #{}, skip cancel", orderId);
            return;
        }

        // Chỉ cho hủy ở trạng thái đầu
        ShippingStatus status = shipment.getCurrentStatus();
        if (status != ShippingStatus.PENDING_PICKUP && status != ShippingStatus.PICKING_UP) {
            throw new BusinessException(
                    "Không thể hủy vận đơn ở trạng thái: " + status.getDescription() +
                            ". Chỉ được hủy khi 'Chờ lấy hàng' hoặc 'Đang lấy hàng'.");
        }

        ShippingCarrierClient client = router.getClient(shipment.getCarrierCode());
        client.cancelShipment(shipment.getExternalOrderCode());

        shipment.setCancelledAt(LocalDateTime.now());
        shipment.setCurrentStatus(ShippingStatus.CANCELLED);
        shipmentRepository.save(shipment);

        log.info("Shipment cancelled for order #{}: {} ({})",
                orderId, shipment.getExternalOrderCode(), shipment.getCarrierCode());
    }

    // ─────────────────────────────────────────────
    // Address master data
    // ─────────────────────────────────────────────

    @Override
    public List<ShippingCarrierClient.LocationItem> getProvinces(CarrierCode carrierCode) {
        return router.getClient(resolveCarrier(carrierCode)).getProvinces();
    }

    @Override
    public List<ShippingCarrierClient.LocationItem> getDistricts(CarrierCode carrierCode, String provinceId) {
        return router.getClient(resolveCarrier(carrierCode)).getDistricts(provinceId);
    }

    @Override
    public List<ShippingCarrierClient.LocationItem> getWards(CarrierCode carrierCode, String districtId) {
        return router.getClient(resolveCarrier(carrierCode)).getWards(districtId);
    }

    @Override
    public List<CarrierCode> getSupportedCarriers() {
        return router.getSupportedCarriers();
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /** Resolve carrier: dùng giá trị truyền vào hoặc fallback về mặc định trong config */
    private CarrierCode resolveCarrier(CarrierCode carrierCode) {
        return carrierCode != null ? carrierCode : shippingProperties.getDefaultCarrier();
    }

    private CreateShipmentRequest buildCreateShipmentRequest(Order order, CarrierCode carrier) {
        // Lấy metadata địa chỉ theo carrier
        String metadataJson = order.getAddress() != null ? order.getAddress().getCarrierMetadata() : null;
        java.util.Map<String, String> addrMeta = metadataHelper.getMetadata(metadataJson, carrier);

        String toName, toPhone, toAddress, toDistrictId, toWardCode;

        if (order.getAddress() != null) {
            toName      = order.getAddress().getReceiverName();
            toPhone     = order.getAddress().getPhone();
            toAddress   = order.getAddress().getDetail() + ", " +
                    order.getAddress().getWard()   + ", " +
                    order.getAddress().getDistrict() + ", " +
                    order.getAddress().getProvince();
            toDistrictId = addrMeta.getOrDefault("districtId", "");
            toWardCode   = addrMeta.getOrDefault("wardCode",   "");
        } else {
            toName       = order.getUser().getFullName();
            toPhone      = order.getUser().getPhone();
            toAddress    = order.getShippingAddress();
            toDistrictId = "";
            toWardCode   = "";
        }

        // Tính trọng lượng
        int totalWeightGram = order.getItems().stream()
                .mapToInt(item -> {
                    BigDecimal weightGram = item.getProduct().getWeight();

                    if (weightGram == null || weightGram.compareTo(BigDecimal.ZERO) <= 0) {
                        return 500; // default 500 gram
                    }

                    // weight trong DB là gram → KHÔNG nhân thêm 1000
                    return weightGram.intValue() * item.getQuantity();
                })
                .sum();

        // Giới hạn an toàn theo GHN
        totalWeightGram = Math.min(totalWeightGram, 30_000); // max 30kg
        int weight = Math.max(totalWeightGram, 100);         // min 100g

        // Build items
        List<ShipmentItem> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            int w = item.getProduct().getWeight() != null
                    ? item.getProduct().getWeight().intValue()
                    : 500;
            items.add(new ShipmentItem(item.getProductName(), item.getQuantity(), w,
                    item.getUnitPrice().intValue()));
        }

        boolean isCod = order.getPaymentMethod() == PaymentMethod.COD;

        return new CreateShipmentRequest(
                toName, toPhone, toAddress,
                null,          // provinceId — không phải tất cả carrier cần
                toDistrictId,
                toWardCode,
                weight, 20, 20, 10,
                order.getSubtotal().intValue(),
                null,          // serviceType — dùng default của mỗi carrier
                order.getNote(),
                isCod,
                isCod ? order.getTotalAmount().intValue() : 0,
                String.valueOf(order.getId()),
                items
        );
    }

    private ShippingTrackingResponse toTrackingResponse(Shipment shipment) {
        List<ShippingTrackingResponse.StatusLogEntry> history = parseStatusLog(shipment.getStatusLog());

        return ShippingTrackingResponse.builder()
                .orderId(shipment.getOrder().getId())
                .carrierCode(shipment.getCarrierCode())
                .carrierName(shipment.getCarrierCode().getDisplayName())
                .externalOrderCode(shipment.getExternalOrderCode())
                .currentStatus(shipment.getCurrentStatus().name())
                .currentStatusLabel(shipment.getCurrentStatus().getDescription())
                .shippingFee(shipment.getShippingFee())
                .totalFee(shipment.getTotalFee())
                .expectedDeliveryTime(shipment.getExpectedDeliveryTime())
                .failReason(shipment.getFailReason())
                .statusHistory(history)
                .createdOnCarrierAt(shipment.getCreatedOnCarrierAt())
                .lastSyncAt(shipment.getLastSyncAt())
                .build();
    }

    private List<ShippingTrackingResponse.StatusLogEntry> parseStatusLog(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<ShippingCarrierClient.StatusLogEntry> logs = objectMapper.readValue(
                    json, new TypeReference<List<ShippingCarrierClient.StatusLogEntry>>() {});
            return logs.stream()
                    .map(l -> ShippingTrackingResponse.StatusLogEntry.builder()
                            .status(l.normalizedStatus().name())
                            .statusLabel(l.normalizedStatus().getDescription())
                            .rawStatus(l.rawStatus())
                            .updatedAt(l.updatedAt())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse status log: {}", e.getMessage());
            return List.of();
        }
    }

    private LocalDateTime parseDateTime(String iso) {
        if (iso == null || iso.isBlank() || "null".equals(iso)) return null;
        try { return OffsetDateTime.parse(iso).toLocalDateTime(); }
        catch (Exception e) {
            try { return LocalDateTime.parse(iso); }
            catch (Exception ex) { return null; }
        }
    }
}