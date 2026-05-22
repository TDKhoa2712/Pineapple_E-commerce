package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.BulkOrderStatusRequest;
import backend.pineapple_ecommerce.dto.request.CreateOrderRequest;
import backend.pineapple_ecommerce.dto.response.OrderResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.entity.*;
import backend.pineapple_ecommerce.enums.*;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.mapper.OrderMapper;
import backend.pineapple_ecommerce.repository.*;
import backend.pineapple_ecommerce.service.CartService;
import backend.pineapple_ecommerce.service.InventoryService;
import backend.pineapple_ecommerce.service.OrderService;
import backend.pineapple_ecommerce.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository           orderRepository;
    private final AddressRepository         addressRepository;
    private final PaymentRepository         paymentRepository;
    private final OrderMapper               orderMapper;
    private final CartService               cartService;
    private final InventoryService inventoryService;

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

        // Sắp xếp theo productId để tránh deadlock
        List<CartItem> sortedCartItems = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();

        for (CartItem cartItem : sortedCartItems) {
            Product product = cartItem.getProduct();
            int qty = cartItem.getQuantity();
            BigDecimal unitPrice = product.getEffectivePrice();

            InventoryBatch usedBatch = inventoryService.deductStockFifo(product.getId(), qty);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .batch(usedBatch)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(qty)))
                    .productName(product.getName())
                    .productThumbnail(product.getThumbnail())
                    .build();

            orderItems.add(item);
            subtotal = subtotal.add(item.getSubtotal());
        }

        BigDecimal shippingFee = calculateShippingFee(subtotal);
        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(subtotal.add(shippingFee).subtract(order.getDiscountAmount()));

        Order savedOrder = orderRepository.save(order);
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
            int page,
            int size) {

        Specification<Order> spec =
                OrderSpecification.hasStatus(status)
                        .and(OrderSpecification.hasUserId(userId))
                        .and(OrderSpecification.hasPaymentMethod(paymentMethod))
                        .and(OrderSpecification.createdBetween(from, to));

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

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

    @Override
    @Transactional
    public int bulkUpdateStatus(BulkOrderStatusRequest request) {
        List<Order> orders = orderRepository.findAllByIdIn(request.getOrderIds());
        AtomicInteger successCount = new AtomicInteger(0);

        orders.forEach(order -> {
            try {
                validateStatusTransition(order.getStatus(), request.getNewStatus());
                order.setStatus(request.getNewStatus());
                orderRepository.save(order);
                successCount.incrementAndGet();
            } catch (BusinessException e) {
                // Bỏ qua đơn không hợp lệ — không throw, tiếp tục xử lý các đơn còn lại
                log.warn("Bulk update skipped orderId={}: {}", order.getId(), e.getMessage());
            }
        });

        log.info("Bulk status update: {}/{} orders updated to {}",
                successCount.get(), request.getOrderIds().size(), request.getNewStatus());
        return successCount.get();
    }

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

    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        return subtotal.compareTo(new BigDecimal("500000")) >= 0
                ? BigDecimal.ZERO
                : new BigDecimal("30000");
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
        boolean valid = switch (current) {
            case PENDING           -> Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED).contains(next);
            case CONFIRMED         -> Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED).contains(next);
            case PROCESSING        -> next == OrderStatus.SHIPPING;
            case SHIPPING          -> next == OrderStatus.DELIVERED;
            case DELIVERED         -> next == OrderStatus.RETURNED;
            case CANCELLED         -> false;
            case RETURNED          -> false;
            case REFUND_REQUESTED  -> next == OrderStatus.REFUNDED;
            case REFUNDED          -> false;
        };

        if (!valid) {
            throw new BusinessException(
                    String.format("Không thể chuyển trạng thái đơn hàng từ %s sang %s", current, next));
        }
    }
}
