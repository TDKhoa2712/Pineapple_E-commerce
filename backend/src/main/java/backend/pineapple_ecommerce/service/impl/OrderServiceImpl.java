package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.CreateOrderRequest;
import backend.pineapple_ecommerce.dto.response.OrderResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.entity.*;
import backend.pineapple_ecommerce.enums.BatchStatus;
import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.enums.PaymentMethod;
import backend.pineapple_ecommerce.enums.PaymentProvider;
import backend.pineapple_ecommerce.enums.PaymentStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.mapper.OrderMapper;
import backend.pineapple_ecommerce.repository.*;
import backend.pineapple_ecommerce.service.CartService;
import backend.pineapple_ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository           orderRepository;
    private final CartRepository            cartRepository;
    private final AddressRepository         addressRepository;
    private final InventoryBatchRepository  inventoryBatchRepository;
    private final PaymentRepository         paymentRepository;
    private final OrderMapper               orderMapper;
    private final CartService               cartService;

    // ─────────────────────────────────────────────
    // CREATE ORDER
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. Lấy cart (kèm items + product)
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng trống, không thể đặt hàng");
        }

        // 2. Lấy & validate địa chỉ
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));

        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Địa chỉ không thuộc về tài khoản của bạn");
        }

        // 3. Tạo order entity
        Order order = Order.builder()
                .user(cart.getUser())
                .address(address)
                .shippingAddress(buildShippingAddressSnapshot(address))
                .paymentMethod(request.getPaymentMethod())
                .note(request.getNote())
                .build();

        // 4. Tạo OrderItems, trừ tồn kho (FIFO: batch gần hết hạn nhất xuất trước)
//        BigDecimal subtotal = BigDecimal.ZERO;
//        List<OrderItem> orderItems = new ArrayList<>();
//
//        for (CartItem cartItem : cart.getItems()) {
//            Product product = cartItem.getProduct();
//            int qty = cartItem.getQuantity();
//            BigDecimal unitPrice = product.getEffectivePrice();
//
//            // Lấy batch AVAILABLE, sắp xếp theo expiryDate tăng dần (FIFO)
//            List<InventoryBatch> batches = inventoryBatchRepository
//                    .findByProductIdAndStatusWithLock(product.getId(), BatchStatus.AVAILABLE);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        //Sắp xếp cart items theo Product ID để tránh Deadlock
        List<CartItem> sortedCartItems = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();

        // Đổi từ cart.getItems() sang sortedCartItems
        for (CartItem cartItem : sortedCartItems) {
            Product product = cartItem.getProduct();
            int qty = cartItem.getQuantity();
            BigDecimal unitPrice = product.getEffectivePrice();

            // Lúc query lock
            List<InventoryBatch> batches = inventoryBatchRepository
                    .findByProductIdAndStatusWithLock(product.getId(), BatchStatus.AVAILABLE);

            int totalStock = batches.stream().mapToInt(InventoryBatch::getRemainingQuantity).sum();
            if (totalStock < qty) {
                throw new BusinessException(
                        String.format("Sản phẩm '%s' chỉ còn %d trong kho", product.getName(), totalStock));
            }

            // Trừ tồn kho theo FIFO
            InventoryBatch usedBatch = deductStockFifo(batches, qty);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .batch(usedBatch)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(qty)))
                    .productName(product.getName())             // snapshot
                    .productThumbnail(product.getThumbnail())  // snapshot
                    .build();

            orderItems.add(item);
            subtotal = subtotal.add(item.getSubtotal());
        }

        // 5. Tính shipping fee (logic đơn giản; mở rộng theo vùng/trọng lượng sau)
        BigDecimal shippingFee = calculateShippingFee(subtotal);

        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(subtotal.add(shippingFee).subtract(order.getDiscountAmount()));

        Order savedOrder = orderRepository.save(order);

        // 6. Tạo Payment record
        createInitialPayment(savedOrder, request.getPaymentMethod());

        // 7. Xoá cart
        cartService.clearCart(userId);

        log.info("Order created: id={}, userId={}, total={}", savedOrder.getId(), userId, savedOrder.getTotalAmount());
        return orderMapper.toResponse(savedOrder);
    }

    // ─────────────────────────────────────────────
    // GET ORDER
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = findOrderWithItems(orderId);

        // User chỉ xem đơn của mình; Admin không bị giới hạn (kiểm tra role ở tầng Controller)
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xem đơn hàng này");
        }
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Long userId, int page, int size) {
        Page<OrderResponse> result = orderRepository
                .findByUserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(orderMapper::toResponse);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = (status != null)
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return PageResponse.of(orderPage.map(orderMapper::toResponse));
    }

    // ─────────────────────────────────────────────
    // UPDATE STATUS (Admin)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = findOrderWithItems(orderId);

        // 1. Lưu lại trạng thái cũ để in log
        OrderStatus oldStatus = order.getStatus();

        // 2. Validate
        validateStatusTransition(oldStatus, newStatus);

        // 3. Cập nhật trạng thái
        order.setStatus(newStatus);

        // 4. Nếu chuyển sang DELIVERED → cập nhật payment status nếu dùng COD
        if (newStatus == OrderStatus.DELIVERED
                && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            paymentRepository.findByOrderId(orderId).ifPresent(p -> {
                p.setStatus(PaymentStatus.PAID);
                paymentRepository.save(p);
            });
        }

        // 5. Nếu chuyển sang REFUNDED → cập nhật payment status
        if (newStatus == OrderStatus.REFUNDED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.findByOrderId(orderId).ifPresent(p -> {
                p.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(p);
            });
        }

        Order saved = orderRepository.save(order);

        // 6. In log chính xác
        log.info("Order {} status updated: {} → {}", orderId, oldStatus, newStatus);

        return orderMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // CANCEL (User)
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

        // *** THÊM: Chặn huỷ nếu đã thanh toán online ***
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

        // 1. Kiểm tra quyền sở hữu
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền thao tác đơn hàng này");
        }

        // 2. Chặn các trường hợp logic sai
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng chưa được thanh toán, không thể yêu cầu hoàn tiền.");
        }

        if (order.getStatus() == OrderStatus.SHIPPING || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Đơn hàng đang được giao hoặc đã giao. Vui lòng sử dụng tính năng Trả Hàng.");
        }

        if (order.getStatus() == OrderStatus.REFUND_REQUESTED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new BusinessException("Đơn hàng này đã được yêu cầu hoàn tiền trước đó.");
        }

        // 3. Xử lý nghiệp vụ: Hoàn tồn kho và đổi trạng thái
        // Tại bước này, hệ thống coi như đơn hàng đã dừng xử lý nên ta hoàn lại tồn kho cho người khác mua.
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

    /**
     * Trừ tồn kho theo FIFO từ danh sách batch đã sắp xếp.
     * Trả về batch bị trừ nhiều nhất (dùng để gán vào OrderItem).
     */
    private InventoryBatch deductStockFifo(List<InventoryBatch> batches, int qty) {
        InventoryBatch mainBatch = null;
        int remaining = qty;

        for (InventoryBatch batch : batches) {
            if (remaining <= 0) break;
            int deduct = Math.min(batch.getRemainingQuantity(), remaining);
            batch.deductStock(deduct);
            inventoryBatchRepository.save(batch);
            remaining -= deduct;

            if (mainBatch == null) mainBatch = batch;
        }

        return mainBatch;
    }

    /** Hoàn lại tồn kho khi huỷ đơn hàng. */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getBatch() != null) {
                InventoryBatch batch = item.getBatch();
                batch.setRemainingQuantity(batch.getRemainingQuantity() + item.getQuantity());
                if (batch.getStatus() == BatchStatus.SOLD_OUT) {
                    batch.setStatus(BatchStatus.AVAILABLE);
                }
                inventoryBatchRepository.save(batch);
            }
        }
    }

    /**
     * Snapshot địa chỉ thành chuỗi để lưu vào đơn hàng.
     * Đảm bảo địa chỉ không thay đổi khi user sửa/xoá sau này.
     */
    private String buildShippingAddressSnapshot(Address address) {
        return String.format("%s - %s - %s, %s, %s, %s",
                address.getReceiverName(),
                address.getPhone(),
                address.getDetail(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince());
    }

    /** Phí ship đơn giản: miễn phí nếu ≥ 500k, ngược lại 30k. */
    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        return subtotal.compareTo(new BigDecimal("500000")) >= 0
                ? BigDecimal.ZERO
                : new BigDecimal("30000");
    }

    /** Tạo Payment record ban đầu khi đặt hàng. */
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

    /**
     * Kiểm tra chuyển trạng thái hợp lệ.
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING          -> Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED).contains(next);
            case CONFIRMED        -> Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED).contains(next);
            case PROCESSING       -> next == OrderStatus.SHIPPING;
            case SHIPPING         -> next == OrderStatus.DELIVERED;
            case DELIVERED        -> next == OrderStatus.RETURNED;
            case CANCELLED        -> false;
            case RETURNED         -> false;
            // LUỒNG REFUND
            case REFUND_REQUESTED -> next == OrderStatus.REFUNDED;
            case REFUNDED         -> false;
        };

        if (!valid) {
            throw new BusinessException(
                    String.format("Không thể chuyển trạng thái đơn hàng từ %s sang %s", current, next));
        }
    }
}
