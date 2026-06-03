package backend.pineapple_ecommerce.modules.order.service;

import backend.pineapple_ecommerce.modules.order.mapper.OrderMapper;
import backend.pineapple_ecommerce.modules.order.repository.OrderRepository;
import backend.pineapple_ecommerce.modules.order.specification.OrderSpecification;
import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.modules.order.models.Order;
import backend.pineapple_ecommerce.modules.order.models.OrderItem;
import backend.pineapple_ecommerce.modules.payment.models.Payment;
import backend.pineapple_ecommerce.modules.payment.repository.PaymentRepository;
import backend.pineapple_ecommerce.modules.order.dto.request.BulkOrderStatusRequest;
import backend.pineapple_ecommerce.modules.order.dto.request.CreateOrderRequest;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import backend.pineapple_ecommerce.common.enums.PaymentMethod;
import backend.pineapple_ecommerce.common.enums.PaymentProvider;
import backend.pineapple_ecommerce.common.enums.PaymentStatus;
import backend.pineapple_ecommerce.modules.address.models.Address;
import backend.pineapple_ecommerce.modules.address.repository.AddressRepository;
import backend.pineapple_ecommerce.modules.cart.models.Cart;
import backend.pineapple_ecommerce.modules.cart.models.CartItem;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.cart.service.CartService;
import backend.pineapple_ecommerce.modules.coupon.service.CouponService;
import backend.pineapple_ecommerce.modules.inventory.service.InventoryService;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.modules.farm.repository.FarmRepository;
import backend.pineapple_ecommerce.modules.order.dto.response.AdminStatisticsResponse;
import backend.pineapple_ecommerce.common.enums.FarmStatus;
import backend.pineapple_ecommerce.common.enums.CarrierCode;
import backend.pineapple_ecommerce.event.OrderCancelledEvent;
import backend.pineapple_ecommerce.infrastructure.carrier.ShippingProviderRouter;
import backend.pineapple_ecommerce.infrastructure.carrier.CarrierAddressMetadataHelper;
import backend.pineapple_ecommerce.infrastructure.carrier.ShippingCarrierClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService, OrderInternalService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final ShippingProviderRouter shippingProviderRouter;
    private final CarrierAddressMetadataHelper metadataHelper;
    private final ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────
    // CREATE ORDER
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        Cart cart = cartService.getCheckoutItems(userId);

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng trống, không thể đặt hàng");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));

        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Địa chỉ không thuộc về tài khoản của bạn");
        }

        // NEW — validate cart trước checkout (2.6)
        var validation = cartService.validateCart(userId);
        if (!validation.isValid()) {
            String problemItems = validation.getWarnings().stream()
                    .map(w -> w.getProductName() + " (" + w.getMessage() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            throw new BusinessException("Giỏ hàng có vấn đề: " + problemItems +
                    ". Vui lòng kiểm tra lại trước khi đặt hàng.");
        }

        Order order = Order.builder()
                .user(cart.getUser())
                .address(address)
                .shippingAddress(buildShippingAddressSnapshot(address))
                .paymentMethod(request.getPaymentMethod())
                .note(request.getNote())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        int totalWeightGram = 0;

        // Sắp xếp theo productId để tránh deadlock
        List<CartItem> sortedCartItems = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();

        for (CartItem cartItem : sortedCartItems) {
            Product product = cartItem.getProduct();
            int qty = cartItem.getQuantity();
            BigDecimal unitPrice = product.getEffectivePrice();

            java.math.BigDecimal itemWeight = product.getWeight();
            if (itemWeight != null) {
                totalWeightGram += itemWeight.multiply(java.math.BigDecimal.valueOf(qty)).intValue();
            } else {
                totalWeightGram += 500 * qty;
            }

            List<InventoryService.BatchAllocation> allocations = inventoryService.deductStockFifo(product.getId(), qty);

            for (InventoryService.BatchAllocation allocation : allocations) {
                OrderItem item = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .batch(allocation.batch())
                        .batchCode(allocation.batch().getBatchCode())
                        .quantity(allocation.quantity())
                        .unitPrice(unitPrice)
                        .subtotal(unitPrice.multiply(BigDecimal.valueOf(allocation.quantity())))
                        .productName(product.getName())
                        .productThumbnail(product.getThumbnail())
                        .build();

                orderItems.add(item);
                subtotal = subtotal.add(item.getSubtotal());
            }
        }

        if (totalWeightGram <= 0) {
            totalWeightGram = 500;
        }
        BigDecimal shippingFee = calculateShippingFee(address, subtotal, totalWeightGram);
        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            discountAmount = couponService.applyAndLock(request.getCouponCode().trim(), userId, cart.getItems(), subtotal);
            order.setDiscountAmount(discountAmount);
            // FIX: lưu couponCode vào Order entity để hiển thị trong OrderResponse
            order.setCouponCode(request.getCouponCode().trim());
        }

        order.setTotalAmount(subtotal.add(shippingFee).subtract(order.getDiscountAmount()));

        Order savedOrder = orderRepository.save(order);

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            couponService.saveCouponUsage(request.getCouponCode().trim(), userId, savedOrder, discountAmount);
        }

        createInitialPayment(savedOrder, request.getPaymentMethod());
        cartService.clearCart(userId);

        log.info("Order created: id={}, userId={}, total={}", savedOrder.getId(), userId, savedOrder.getTotalAmount());
        return orderMapper.toResponse(savedOrder);
    }

    // ─────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = findOrderWithItems(orderId);
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xem đơn hàng này");
        }
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAsAdmin(Long orderId) {
        Order order = findOrderWithItems(orderId);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Long userId, OrderStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> result = (status != null)
                ? orderRepository.findByUserIdAndStatus(userId, status, pageable)
                : orderRepository.findByUserId(userId, pageable);
        return PageResponse.of(result.map(orderMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(
            OrderStatus status,
            Long userId,
            PaymentMethod paymentMethod,
            LocalDateTime from,
            LocalDateTime to,
            String keyword,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        Specification<Order> spec =
                OrderSpecification.hasStatus(status)
                        .and(OrderSpecification.hasUserId(userId))
                        .and(OrderSpecification.hasPaymentMethod(paymentMethod))
                        .and(OrderSpecification.createdBetween(from, to))
                        .and(OrderSpecification.searchKeyword(keyword));

        Sort sort = Sort.by("createdAt").descending();
        if (sortBy != null && !sortBy.isBlank()) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, sortBy);
        }

        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        return PageResponse.of(orderPage.map(orderMapper::toResponse));
    }

    // ─────────────────────────────────────────────
    // UPDATE STATUS
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = findOrderWithItems(orderId);
        OrderStatus oldStatus = order.getStatus();

        validateStatusTransition(oldStatus, newStatus);
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
            couponService.releaseCouponUsage(orderId);

            if (oldStatus == OrderStatus.PROCESSING) {
                eventPublisher.publishEvent(new OrderCancelledEvent(orderId));
            }
        }

        if (newStatus == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            paymentRepository.findByOrderId(orderId).ifPresent(p -> {
                p.setStatus(PaymentStatus.PAID);
                paymentRepository.save(p);
            });
        }

        if (newStatus == OrderStatus.REFUNDED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.findByOrderId(orderId).ifPresent(p -> {
                p.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(p);
            });
        }

        Order saved = orderRepository.save(order);
        log.info("Order {} status: {} → {}", orderId, oldStatus, newStatus);
        return orderMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // NEW — 2.1: BULK UPDATE STATUS
    // ─────────────────────────────────────────────

    // ─────────────────────────────────────────────
    // CANCEL & REFUND
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = findOrderWithItems(orderId);

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền huỷ đơn hàng này");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Chỉ có thể huỷ đơn hàng khi đang ở trạng thái PENDING");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID
                && order.getPaymentMethod() != PaymentMethod.COD) {
            throw new BusinessException(
                    "Đơn hàng đã được thanh toán. " +
                    "Vui lòng liên hệ hỗ trợ để được hoàn tiền trong 3-5 ngày làm việc.");
        }

        restoreStock(order);
        couponService.releaseCouponUsage(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order {} cancelled by userId={}", orderId, userId);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse requestRefund(Long orderId, Long userId) {
        Order order = findOrderWithItems(orderId);

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền thao tác đơn hàng này");
        }
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng chưa được thanh toán, không thể yêu cầu hoàn tiền.");
        }
        if (order.getStatus() == OrderStatus.SHIPPING || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Đơn hàng đang được giao hoặc đã giao. Vui lòng sử dụng tính năng Trả Hàng.");
        }
        if (order.getStatus() == OrderStatus.REFUND_REQUESTED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new BusinessException("Đơn hàng này đã được yêu cầu hoàn tiền trước đó.");
        }

        restoreStock(order);
        order.setStatus(OrderStatus.REFUND_REQUESTED);
        Order saved = orderRepository.save(order);
        log.info("Order {} refund requested by userId={}", orderId, userId);
        return orderMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    // ─────────────────────────────────────────────
    // ORDER INTERNAL SERVICE IMPLEMENTATION
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional
    public boolean updateShippingFeeIfNeeded(Long orderId, BigDecimal shippingFee) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (order.getShippingFee() == null || order.getShippingFee().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal fee = shippingFee != null ? shippingFee : BigDecimal.ZERO;
            order.setShippingFee(fee);
            BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
            order.setTotalAmount(order.getSubtotal().add(fee).subtract(discount));
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    private Order findOrderWithItems(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private void restoreStock(Order order) {
        inventoryService.restoreStockForOrder(order.getItems());
    }

    private String buildShippingAddressSnapshot(Address address) {
        return String.format("%s - %s - %s, %s, %s, %s",
                address.getReceiverName(), address.getPhone(),
                address.getDetail(), address.getWard(),
                address.getDistrict(), address.getProvince());
    }

    private BigDecimal calculateShippingFee(Address address, BigDecimal subtotal, int weightGram) {
        if (subtotal.compareTo(new BigDecimal("500000")) >= 0) {
            return BigDecimal.ZERO;
        }

        String districtId = metadataHelper.getGhnDistrictId(address.getCarrierMetadata());
        String wardCode = metadataHelper.getGhnWardCode(address.getCarrierMetadata());

        if (districtId != null && !districtId.isBlank() && wardCode != null && !wardCode.isBlank()) {
            try {
                ShippingCarrierClient client = shippingProviderRouter.getClient(CarrierCode.GHN);
                ShippingCarrierClient.FeeRequest feeReq = new ShippingCarrierClient.FeeRequest(
                        null,
                        districtId,
                        wardCode,
                        weightGram, // weightGram
                        20,  // lengthCm
                        20,  // widthCm
                        10,  // heightCm
                        0,   // insuranceValue
                        null, // serviceTypeId
                        null  // coupon
                );
                ShippingCarrierClient.FeeResult result = client.calculateFee(feeReq);
                if (result != null && result.totalFee() != null) {
                    return result.totalFee();
                }
            } catch (Exception e) {
                log.warn("Lỗi tính phí giao hàng GHN tự động, sử dụng phí mặc định 30,000đ: {}", e.getMessage());
            }
        }

        return new BigDecimal("30000");
    }

    private void createInitialPayment(Order order, PaymentMethod method) {
        PaymentProvider provider = switch (method) {
            case COD           -> PaymentProvider.COD;
            case VNPAY         -> PaymentProvider.VNPAY;
            case MOMO          -> PaymentProvider.MOMO;
            case BANK_TRANSFER -> PaymentProvider.BANK;
        };

        Payment payment = Payment.builder()
                .order(order)
                .provider(provider)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.UNPAID)
                .build();

        paymentRepository.save(payment);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (!current.canTransitionTo(next)) {
            throw new BusinessException(
                    String.format("Không thể chuyển trạng thái đơn hàng từ %s sang %s", current, next));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatisticsResponse getAdminStatistics() {
        long totalUsers = userRepository.count();
        long pendingFarms = farmRepository.countByStatus(FarmStatus.PENDING_APPROVAL);

        long totalOrdersCount = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByStatusNot(OrderStatus.CANCELLED);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        java.util.Map<String, Long> statusDistribution = new java.util.HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusDistribution.put(status.name(), 0L);
        }
        List<Object[]> statusCounts = orderRepository.countOrdersByStatus();
        for (Object[] row : statusCounts) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = (Long) row[1];
            statusDistribution.put(status.name(), count);
        }

        java.util.Map<String, AdminStatisticsResponse.MonthlyRevenue> monthlyMap = new java.util.LinkedHashMap<>();
        java.time.format.DateTimeFormatter monthFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDateTime now = LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            String m = now.minusMonths(i).format(monthFormatter);
            monthlyMap.put(m, new AdminStatisticsResponse.MonthlyRevenue(m, BigDecimal.ZERO, 0L));
        }

        LocalDateTime cutoff = now.minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay();
        List<Object[]> monthlyStats = orderRepository.getMonthlyRevenueAndCount(cutoff, OrderStatus.CANCELLED);
        for (Object[] row : monthlyStats) {
            String monthStr = (String) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            Long count = (Long) row[2];
            if (monthlyMap.containsKey(monthStr)) {
                AdminStatisticsResponse.MonthlyRevenue mr = monthlyMap.get(monthStr);
                mr.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);
                mr.setOrderCount(count != null ? count : 0L);
            } else {
                monthlyMap.put(monthStr, new AdminStatisticsResponse.MonthlyRevenue(monthStr, revenue != null ? revenue : BigDecimal.ZERO, count != null ? count : 0L));
            }
        }

        String currentMonthStr = now.format(monthFormatter);
        String prevMonthStr = now.minusMonths(1).format(monthFormatter);

        AdminStatisticsResponse.MonthlyRevenue currentMonthData = monthlyMap.get(currentMonthStr);
        AdminStatisticsResponse.MonthlyRevenue prevMonthData = monthlyMap.get(prevMonthStr);

        BigDecimal currentRevenue = currentMonthData != null ? currentMonthData.getRevenue() : BigDecimal.ZERO;
        BigDecimal prevRevenue = prevMonthData != null ? prevMonthData.getRevenue() : BigDecimal.ZERO;
        long currentOrderCount = currentMonthData != null ? currentMonthData.getOrderCount() : 0L;
        long prevOrderCount = prevMonthData != null ? prevMonthData.getOrderCount() : 0L;

        double revenueChangePercentage = 0.0;
        if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueChangePercentage = currentRevenue.subtract(prevRevenue)
                    .divide(prevRevenue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        } else if (currentRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueChangePercentage = 100.0;
        }

        double orderChangePercentage = 0.0;
        if (prevOrderCount > 0) {
            orderChangePercentage = ((double) (currentOrderCount - prevOrderCount) / prevOrderCount) * 100.0;
        } else if (currentOrderCount > 0) {
            orderChangePercentage = 100.0;
        }

        return AdminStatisticsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrdersCount)
                .totalUsers(totalUsers)
                .pendingFarms(pendingFarms)
                .revenueChangePercentage(revenueChangePercentage)
                .orderChangePercentage(orderChangePercentage)
                .monthlyRevenueList(new ArrayList<>(monthlyMap.values()))
                .orderStatusDistribution(statusDistribution)
                .build();
    }
}
