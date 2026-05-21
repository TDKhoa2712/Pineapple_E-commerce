package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.config.GhnProperties;
import backend.pineapple_ecommerce.dto.ghn.GhnApiDto;
import backend.pineapple_ecommerce.dto.request.CalculateShippingFeeRequest;
import backend.pineapple_ecommerce.dto.response.ShippingFeeResponse;
import backend.pineapple_ecommerce.dto.response.ShippingTrackingResponse;
import backend.pineapple_ecommerce.entity.GhnShipment;
import backend.pineapple_ecommerce.entity.Order;
import backend.pineapple_ecommerce.entity.OrderItem;
import backend.pineapple_ecommerce.enums.GhnShippingStatus;
import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.enums.PaymentMethod;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.repository.GhnShipmentRepository;
import backend.pineapple_ecommerce.repository.OrderRepository;
import backend.pineapple_ecommerce.service.GhnApiClient;
import backend.pineapple_ecommerce.service.GhnShippingService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnShippingServiceImpl implements GhnShippingService {

    private final GhnApiClient ghnApiClient;
    private final GhnProperties ghnProperties;
    private final GhnShipmentRepository ghnShipmentRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────
    // 1. Calculate Fee
    // ─────────────────────────────────────────────

    @Override
    public ShippingFeeResponse calculateFee(CalculateShippingFeeRequest request) {
        log.info("Calculating shipping fee: district={}, ward={}, weight={}",
                request.getToDistrictId(), request.getToWardCode(), request.getWeight());

        GhnApiDto.FeeData feeData = ghnApiClient.calculateFee(
                request.getToDistrictId(),
                request.getToWardCode(),
                request.getWeight(),
                request.getLength(),
                request.getWidth(),
                request.getHeight(),
                request.getInsuranceValue(),
                request.getServiceTypeId(),
                request.getCoupon()
        );

        return ShippingFeeResponse.builder()
                .serviceFee(toBigDecimal(feeData.getServiceFee()))
                .insuranceFee(toBigDecimal(feeData.getInsuranceFee()))
                .totalFee(toBigDecimal(feeData.getTotal()))
                .codFee(toBigDecimal(feeData.getCodFee()))
                .couponDiscount(toBigDecimal(feeData.getCouponValue()))
                .serviceTypeId(request.getServiceTypeId())
                .build();
    }

    // ─────────────────────────────────────────────
    // 2. Create Shipment
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ShippingTrackingResponse createShipment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        // Kiểm tra trạng thái — chỉ tạo vận đơn khi đang PROCESSING
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new BusinessException(
                    "Chỉ tạo vận đơn khi đơn hàng ở trạng thái PROCESSING, hiện tại: " + order.getStatus());
        }

        // Kiểm tra đã tạo vận đơn chưa
        if (ghnShipmentRepository.existsByOrderId(orderId)) {
            GhnShipment existing = ghnShipmentRepository.findByOrderId(orderId).get();
            if (existing.isActive()) {
                throw new BusinessException("Đơn hàng #" + orderId + " đã có vận đơn GHN: " + existing.getGhnOrderCode());
            }
        }

        // Build request gửi GHN
        GhnApiDto.CreateOrderRequest ghnRequest = buildCreateOrderRequest(order);
        GhnApiDto.CreateOrderData result = ghnApiClient.createOrder(ghnRequest);

        log.info("GHN order created: orderCode={}, fee={}", result.getOrderCode(), result.getTotalFee());

        // Lưu vào DB
        GhnShipment shipment = GhnShipment.builder()
                .order(order)
                .ghnOrderCode(result.getOrderCode())
                .clientOrderCode(String.valueOf(orderId))
                .sortCode(result.getSortCode())
                .currentStatus(GhnShippingStatus.READY_TO_PICK)
                .shippingFee(parseFee(result.getTotalFee()))
                .totalFee(parseFee(result.getTotalFee()))
                .expectedDeliveryTime(parseDateTime(result.getExpectedDeliveryTime()))
                .createdOnGhnAt(LocalDateTime.now())
                .lastSyncAt(LocalDateTime.now())
                .build();

        ghnShipmentRepository.save(shipment);

        // Cập nhật shippingFee vào Order nếu chưa có
        if (order.getShippingFee().compareTo(BigDecimal.ZERO) == 0) {
            order.setShippingFee(shipment.getShippingFee());
            order.setTotalAmount(order.getSubtotal().add(shipment.getShippingFee()));
            orderRepository.save(order);
        }

        return toTrackingResponse(shipment);
    }

    // ─────────────────────────────────────────────
    // 3. Get Tracking
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ShippingTrackingResponse getTracking(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        // Authorize — chỉ xem đơn của mình
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xem thông tin này");
        }

        GhnShipment shipment = ghnShipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Đơn hàng #" + orderId + " chưa có vận đơn giao hàng"));

        return toTrackingResponse(shipment);
    }

    // ─────────────────────────────────────────────
    // 4. Sync Status (từ Webhook hoặc Scheduler)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void syncStatusFromGhn(String ghnOrderCode) {
        GhnShipment shipment = ghnShipmentRepository.findByGhnOrderCode(ghnOrderCode)
                .orElseGet(() -> {
                    log.warn("Received GHN webhook for unknown order: {}", ghnOrderCode);
                    return null;
                });

        if (shipment == null) return;

        try {
            GhnApiDto.OrderInfoData info = ghnApiClient.getOrderInfo(ghnOrderCode);
            GhnShippingStatus newStatus = GhnShippingStatus.fromCode(info.getStatus());

            log.info("Syncing GHN status: orderCode={}, status={} → {}",
                    ghnOrderCode, shipment.getCurrentStatus(), newStatus);

            // Cập nhật shipment
            shipment.setCurrentStatus(newStatus);
            shipment.setLastSyncAt(LocalDateTime.now());

            if (info.getLog() != null) {
                shipment.setStatusLog(objectMapper.writeValueAsString(info.getLog()));
            }

            // Cập nhật OrderStatus nội bộ nếu có mapping
            OrderStatus mappedOrderStatus = newStatus.toOrderStatus();
            if (mappedOrderStatus != null) {
                Order order = shipment.getOrder();
                if (order.getStatus().canTransitionTo(mappedOrderStatus)) {
                    order.setStatus(mappedOrderStatus);
                    orderRepository.save(order);
                    log.info("Updated order #{} status to {}", order.getId(), mappedOrderStatus);
                }
            }

            ghnShipmentRepository.save(shipment);

        } catch (Exception e) {
            log.error("Failed to sync GHN status for {}: {}", ghnOrderCode, e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    // 5. Cancel Shipment
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void cancelShipment(Long orderId) {
        GhnShipment shipment = ghnShipmentRepository.findByOrderId(orderId)
                .orElse(null);

        if (shipment == null || !shipment.isActive()) {
            log.info("No active GHN shipment found for order #{}, skip cancel", orderId);
            return;
        }

        // GHN chỉ cho hủy khi đang ready_to_pick hoặc picking
        GhnShippingStatus status = shipment.getCurrentStatus();
        if (status != GhnShippingStatus.READY_TO_PICK && status != GhnShippingStatus.PICKING) {
            throw new BusinessException(
                    "Không thể hủy vận đơn GHN ở trạng thái: " + status.getDescription() +
                    ". Chỉ được hủy khi đang 'Chờ lấy hàng' hoặc 'Đang lấy hàng'.");
        }

        ghnApiClient.cancelOrder(shipment.getGhnOrderCode());

        shipment.setCancelledAt(LocalDateTime.now());
        shipment.setCurrentStatus(GhnShippingStatus.CANCEL);
        ghnShipmentRepository.save(shipment);

        log.info("GHN shipment cancelled for order #{}: {}", orderId, shipment.getGhnOrderCode());
    }

    // ─────────────────────────────────────────────
    // 6. Address Data
    // ─────────────────────────────────────────────

    @Override
    public List<GhnApiDto.Province> getProvinces() {
        return ghnApiClient.getProvinces();
    }

    @Override
    public List<GhnApiDto.District> getDistricts(Integer provinceId) {
        return ghnApiClient.getDistricts(provinceId);
    }

    @Override
    public List<GhnApiDto.Ward> getWards(Integer districtId) {
        return ghnApiClient.getWards(districtId);
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /**
     * Build GHN CreateOrderRequest từ Order entity.
     *
     * <p>Lưu ý: fromAddress phải là địa chỉ shop (lấy từ GhnProperties hoặc hardcode).
     * Hiện tại dùng placeholder — cần config trong application.yml:
     *   app.ghn.shop-address, app.ghn.shop-province, ...
     */
    private GhnApiDto.CreateOrderRequest buildCreateOrderRequest(Order order) {
        GhnApiDto.CreateOrderRequest req = new GhnApiDto.CreateOrderRequest();

        // Người nhận
        if (order.getAddress() != null) {
            req.setToName(order.getAddress().getReceiverName());
            req.setToPhone(order.getAddress().getPhone());
            req.setToAddress(order.getAddress().getDetail() + ", " +
                             order.getAddress().getWard() + ", " +
                             order.getAddress().getDistrict() + ", " +
                             order.getAddress().getProvince());
            // toWardCode và toDistrictId cần được lưu trong Address entity
            // Xem phần "Cập nhật Address entity" trong hướng dẫn
            req.setToWardCode(order.getAddress().getGhnWardCode());
            req.setToDistrictId(order.getAddress().getGhnDistrictId());
        } else {
            // Fallback: parse từ shippingAddress snapshot string
            req.setToName(order.getUser().getFullName());
            req.setToPhone(order.getUser().getPhone());
            req.setToAddress(order.getShippingAddress());
        }

        // COD
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            req.setPaymentTypeId(2); // Người nhận trả phí ship
            req.setCodAmount(order.getTotalAmount().intValue());
        } else {
            req.setPaymentTypeId(1); // Shop trả phí ship
            req.setCodAmount(0);
        }

        // Khối lượng — tính tổng từ items (fallback 500g nếu sản phẩm không có weight)
        int totalWeightGram = order.getItems().stream()
                .mapToInt(item -> {
                    BigDecimal weightKg = item.getProduct().getWeight();
                    if (weightKg == null) return 500; // 500g default
                    return weightKg.multiply(BigDecimal.valueOf(1000)).intValue() * item.getQuantity();
                })
                .sum();
        req.setWeight(Math.max(totalWeightGram, 100)); // Tối thiểu 100g

        req.setNote(order.getNote());
        req.setClientOrderCode(String.valueOf(order.getId()));
        req.setInsuranceValue(order.getSubtotal().intValue());

        // Items
        List<GhnApiDto.CreateOrderRequest.OrderItem> ghnItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            GhnApiDto.CreateOrderRequest.OrderItem ghnItem = new GhnApiDto.CreateOrderRequest.OrderItem();
            ghnItem.setName(item.getProductName());
            ghnItem.setQuantity(item.getQuantity());
            ghnItem.setPrice(item.getUnitPrice().intValue());
            int w = item.getProduct().getWeight() != null
                    ? item.getProduct().getWeight().multiply(BigDecimal.valueOf(1000)).intValue()
                    : 500;
            ghnItem.setWeight(w);
            ghnItems.add(ghnItem);
        }
        req.setItems(ghnItems);

        return req;
    }

    private ShippingTrackingResponse toTrackingResponse(GhnShipment shipment) {
        List<ShippingTrackingResponse.StatusLogEntry> history = parseStatusLog(shipment.getStatusLog());

        return ShippingTrackingResponse.builder()
                .orderId(shipment.getOrder().getId())
                .ghnOrderCode(shipment.getGhnOrderCode())
                .currentStatus(shipment.getCurrentStatus().getCode())
                .currentStatusLabel(shipment.getCurrentStatus().getDescription())
                .shippingFee(shipment.getShippingFee())
                .totalFee(shipment.getTotalFee())
                .expectedDeliveryTime(shipment.getExpectedDeliveryTime())
                .failReason(shipment.getFailReason())
                .statusHistory(history)
                .createdOnGhnAt(shipment.getCreatedOnGhnAt())
                .lastSyncAt(shipment.getLastSyncAt())
                .build();
    }

    private List<ShippingTrackingResponse.StatusLogEntry> parseStatusLog(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<GhnApiDto.StatusLog> logs = objectMapper.readValue(json,
                    new TypeReference<List<GhnApiDto.StatusLog>>() {});
            return logs.stream()
                    .map(l -> ShippingTrackingResponse.StatusLogEntry.builder()
                            .status(l.getStatus())
                            .statusLabel(GhnShippingStatus.fromCode(l.getStatus()).getDescription())
                            .updatedAt(parseDateTime(l.getUpdatedDate()))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse status log: {}", e.getMessage());
            return List.of();
        }
    }

    private BigDecimal toBigDecimal(Integer value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private BigDecimal parseFee(String value) {
        if (value == null) return BigDecimal.ZERO;
        try { return new BigDecimal(value.replaceAll("[^0-9.]", "")); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private LocalDateTime parseDateTime(String iso) {
        if (iso == null || iso.isBlank() || iso.equals("null")) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception e) {
            try { return LocalDateTime.parse(iso); }
            catch (Exception ex) { return null; }
        }
    }
}
