//package backend.pineapple_ecommerce.service.impl;
//
//import backend.pineapple_ecommerce.dto.request.CreateOrderRequest;
//import backend.pineapple_ecommerce.dto.response.OrderResponse;
//import backend.pineapple_ecommerce.dto.response.PageResponse;
//import backend.pineapple_ecommerce.entity.*;
//import backend.pineapple_ecommerce.enums.*;
//import backend.pineapple_ecommerce.exception.BusinessException;
//import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
//import backend.pineapple_ecommerce.exception.UnauthorizedException;
//import backend.pineapple_ecommerce.mapper.OrderMapper;
//import backend.pineapple_ecommerce.repository.*;
//import backend.pineapple_ecommerce.service.CartService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.CsvSource;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("OrderServiceImpl")
//class OrderServiceImplTest {
//
//    @Mock private OrderRepository          orderRepository;
//    @Mock private CartRepository           cartRepository;
//    @Mock private AddressRepository        addressRepository;
//    @Mock private InventoryBatchRepository inventoryBatchRepository;
//    @Mock private PaymentRepository        paymentRepository;
//    @Mock private OrderMapper              orderMapper;
//    @Mock private CartService             cartService;
//
//    @InjectMocks
//    private OrderServiceImpl orderService;
//
//    // ── Fixtures ──────────────────────────────────────────────────────
//
//    private static final Long USER_ID    = 1L;
//    private static final Long ORDER_ID   = 100L;
//    private static final Long ADDRESS_ID = 10L;
//    private static final Long PRODUCT_ID = 20L;
//    private static final Long BATCH_ID   = 30L;
//
//    private User             user;
//    private Address          address;
//    private Product          product;
//    private InventoryBatch   batch;
//    private Cart             cart;
//    private CartItem         cartItem;
//    private Order            pendingOrder;
//    private OrderItem        orderItem;
//    private OrderResponse    orderResponse;
//
//    @BeforeEach
//    void setUp() {
//        user = User.builder().id(USER_ID).email("user@example.com").build();
//
//        address = Address.builder()
//                .id(ADDRESS_ID)
//                .user(user)
//                .receiverName("Nguyen Van A")
//                .phone("0901234567")
//                .province("TP.HCM")
//                .district("Quận 1")
//                .ward("Phường Bến Nghé")
//                .detail("123 Lê Lợi")
//                .isDefault(true)
//                .build();
//
//        product = Product.builder()
//                .id(PRODUCT_ID)
//                .name("Dứa mật vàng Cầu Đúc")
//                .price(new BigDecimal("50000"))
//                .status(ProductStatus.ACTIVE)
//                .build();
//
//        // Product.getEffectivePrice() → discountPrice nếu có, ngược lại price
//        // Ở đây không set discountPrice → effectivePrice = price = 50_000
//
//        batch = InventoryBatch.builder()
//                .id(BATCH_ID)
//                .product(product)
//                .remainingQuantity(100)
//                .status(BatchStatus.AVAILABLE)
//                .build();
//
//        cartItem = CartItem.builder()
//                .id(1L)
//                .product(product)
//                .quantity(2)
//                .build();
//
//        cart = Cart.builder()
//                .id(50L)
//                .user(user)
//                .items(new ArrayList<>(List.of(cartItem)))
//                .build();
//
//        cartItem.setCart(cart);   // bi-directional
//
//        orderItem = OrderItem.builder()
//                .id(1L)
//                .product(product)
//                .batch(batch)
//                .quantity(2)
//                .unitPrice(new BigDecimal("50000"))
//                .subtotal(new BigDecimal("100000"))
//                .productName("Dứa mật vàng Cầu Đúc")
//                .build();
//
//        pendingOrder = Order.builder()
//                .id(ORDER_ID)
//                .user(user)
//                .address(address)
//                .status(OrderStatus.PENDING)
//                .paymentStatus(PaymentStatus.UNPAID)
//                .paymentMethod(PaymentMethod.COD)
//                .subtotal(new BigDecimal("100000"))
//                .shippingFee(new BigDecimal("30000"))
//                .discountAmount(BigDecimal.ZERO)
//                .totalAmount(new BigDecimal("130000"))
//                .items(new ArrayList<>(List.of(orderItem)))
//                .build();
//
//        orderResponse = OrderResponse.builder()
//                .id(ORDER_ID)
//                .status("PENDING")
//                .build();
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    // createOrder
//    // ─────────────────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("createOrder()")
//    class CreateOrder {
//
//        private CreateOrderRequest buildRequest() {
//            CreateOrderRequest req = new CreateOrderRequest();
//            req.setAddressId(ADDRESS_ID);
//            req.setPaymentMethod(PaymentMethod.COD);
//            req.setNote("Giao buổi sáng");
//            return req;
//        }
//
//        @Test
//        @DisplayName("tạo đơn hàng COD thành công → trả về OrderResponse")
//        void givenValidRequest_shouldCreateOrderAndReturnResponse() {
//            CreateOrderRequest req = buildRequest();
//            Order savedOrder = pendingOrder;
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch));
//            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
//            when(orderMapper.toResponse(savedOrder)).thenReturn(orderResponse);
//
//            OrderResponse result = orderService.createOrder(USER_ID, req);
//
//            assertThat(result).isSameAs(orderResponse);
//            verify(paymentRepository).save(any(Payment.class));
//            verify(cartService).clearCart(USER_ID);
//        }
//
//        @Test
//        @DisplayName("subtotal < 500k → phí ship = 30.000đ")
//        void givenSubtotalUnder500k_shouldChargeShippingFee() {
//            // subtotal = 2 × 50.000 = 100.000 < 500.000 → ship 30.000
//            CreateOrderRequest req = buildRequest();
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch));
//            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(orderMapper.toResponse(any())).thenReturn(orderResponse);
//
//            orderService.createOrder(USER_ID, req);
//
//            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
//            verify(orderRepository).save(orderCaptor.capture());
//            assertThat(orderCaptor.getValue().getShippingFee())
//                    .isEqualByComparingTo(new BigDecimal("30000"));
//        }
//
//        @Test
//        @DisplayName("subtotal >= 500k → phí ship = 0đ (miễn phí)")
//        void givenSubtotalAtLeast500k_shouldHaveZeroShipping() {
//            // Tăng giá sản phẩm để đạt 500.000: 2 × 300.000 = 600.000
//            product.setPrice(new BigDecimal("300000"));
//            CreateOrderRequest req = buildRequest();
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch));
//            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(orderMapper.toResponse(any())).thenReturn(orderResponse);
//
//            orderService.createOrder(USER_ID, req);
//
//            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
//            verify(orderRepository).save(captor.capture());
//            assertThat(captor.getValue().getShippingFee())
//                    .isEqualByComparingTo(BigDecimal.ZERO);
//        }
//
//        @Test
//        @DisplayName("tạo đơn thành công → tồn kho bị trừ theo FIFO")
//        void givenOrder_shouldDeductInventoryFifo() {
//            CreateOrderRequest req = buildRequest(); // qty = 2
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch));
//            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(orderMapper.toResponse(any())).thenReturn(orderResponse);
//
//            orderService.createOrder(USER_ID, req);
//
//            // batch ban đầu 100, trừ 2 → còn 98
//            assertThat(batch.getRemainingQuantity()).isEqualTo(98);
//            verify(inventoryBatchRepository).save(batch);
//        }
//
//        @Test
//        @DisplayName("tạo đơn → Payment ban đầu có status UNPAID")
//        void givenOrder_shouldCreatePaymentWithUnpaidStatus() {
//            CreateOrderRequest req = buildRequest();
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch));
//            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(orderMapper.toResponse(any())).thenReturn(orderResponse);
//
//            orderService.createOrder(USER_ID, req);
//
//            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
//            verify(paymentRepository).save(paymentCaptor.capture());
//            assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.UNPAID);
//            assertThat(paymentCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.COD);
//        }
//
//        @Test
//        @DisplayName("giỏ hàng trống → ném BusinessException")
//        void givenEmptyCart_shouldThrowBusinessException() {
//            cart.getItems().clear();
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//
//            assertThatThrownBy(() -> orderService.createOrder(USER_ID, buildRequest()))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("trống");
//        }
//
//        @Test
//        @DisplayName("cart không tồn tại → ném ResourceNotFoundException")
//        void givenMissingCart_shouldThrow() {
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> orderService.createOrder(USER_ID, buildRequest()))
//                    .isInstanceOf(ResourceNotFoundException.class);
//        }
//
//        @Test
//        @DisplayName("addressId không tồn tại → ném ResourceNotFoundException")
//        void givenMissingAddress_shouldThrow() {
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> orderService.createOrder(USER_ID, buildRequest()))
//                    .isInstanceOf(ResourceNotFoundException.class);
//        }
//
//        @Test
//        @DisplayName("địa chỉ thuộc user khác → ném UnauthorizedException")
//        void givenAddressOfOtherUser_shouldThrowUnauthorizedException() {
//            User otherUser = User.builder().id(99L).build();
//            address.setUser(otherUser);
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//
//            assertThatThrownBy(() -> orderService.createOrder(USER_ID, buildRequest()))
//                    .isInstanceOf(UnauthorizedException.class);
//        }
//
//        @Test
//        @DisplayName("tồn kho không đủ → ném BusinessException chứa tên sản phẩm")
//        void givenInsufficientStock_shouldThrowWithProductName() {
//            cartItem.setQuantity(200); // yêu cầu 200, batch chỉ có 100
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch));
//
//            assertThatThrownBy(() -> orderService.createOrder(USER_ID, buildRequest()))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("Dứa mật vàng Cầu Đúc")
//                    .hasMessageContaining("100");
//        }
//
//        @Test
//        @DisplayName("FIFO trải qua nhiều batch khi 1 batch không đủ")
//        void givenMultipleBatches_shouldDeductFifo() {
//            // Batch 1: 1 cái, Batch 2: 50 cái; order qty = 3 → lấy 1 từ batch1, 2 từ batch2
//            InventoryBatch batch1 = InventoryBatch.builder()
//                    .id(31L).product(product).remainingQuantity(1).status(BatchStatus.AVAILABLE).build();
//            InventoryBatch batch2 = InventoryBatch.builder()
//                    .id(32L).product(product).remainingQuantity(50).status(BatchStatus.AVAILABLE).build();
//
//            cartItem.setQuantity(3);
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch1, batch2));
//            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(orderMapper.toResponse(any())).thenReturn(orderResponse);
//
//            orderService.createOrder(USER_ID, buildRequest());
//
//            // batch1: 1-1=0 → SOLD_OUT; batch2: 50-2=48
//            assertThat(batch1.getRemainingQuantity()).isZero();
//            assertThat(batch1.getStatus()).isEqualTo(BatchStatus.SOLD_OUT);
//            assertThat(batch2.getRemainingQuantity()).isEqualTo(48);
//        }
//
//        @Test
//        @DisplayName("tạo đơn thành công → cart bị xoá")
//        void givenSuccessfulOrder_shouldClearCart() {
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch));
//            when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);
//            when(orderMapper.toResponse(any())).thenReturn(orderResponse);
//
//            orderService.createOrder(USER_ID, buildRequest());
//
//            verify(cartService).clearCart(USER_ID);
//        }
//
//        @Test
//        @DisplayName("payment method VNPAY → provider = VNPAY")
//        void givenVnpayMethod_shouldCreateVnpayPayment() {
//            CreateOrderRequest req = buildRequest();
//            req.setPaymentMethod(PaymentMethod.VNPAY);
//
//            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
//            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
//            when(inventoryBatchRepository.findByProductIdAndStatusWithLock(PRODUCT_ID, BatchStatus.AVAILABLE))
//                    .thenReturn(List.of(batch));
//            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(orderMapper.toResponse(any())).thenReturn(orderResponse);
//
//            orderService.createOrder(USER_ID, req);
//
//            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
//            verify(paymentRepository).save(paymentCaptor.capture());
//            assertThat(paymentCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.VNPAY);
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    // getOrderById
//    // ─────────────────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("getOrderById()")
//    class GetOrderById {
//
//        @Test
//        @DisplayName("đúng chủ đơn hàng → trả về OrderResponse")
//        void givenOwnerRequest_shouldReturnResponse() {
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            OrderResponse result = orderService.getOrderById(ORDER_ID, USER_ID);
//
//            assertThat(result).isSameAs(orderResponse);
//        }
//
//        @Test
//        @DisplayName("user khác xem đơn hàng → ném UnauthorizedException")
//        void givenOtherUserRequest_shouldThrowUnauthorizedException() {
//            Long otherUserId = 99L;
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID, otherUserId))
//                    .isInstanceOf(UnauthorizedException.class);
//        }
//
//        @Test
//        @DisplayName("orderId không tồn tại → ném ResourceNotFoundException")
//        void givenMissingOrder_shouldThrow() {
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID, USER_ID))
//                    .isInstanceOf(ResourceNotFoundException.class);
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    // getMyOrders
//    // ─────────────────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("getMyOrders()")
//    class GetMyOrders {
//
//        @Test
//        @DisplayName("trả về PageResponse đúng page/size")
//        void givenUserId_shouldReturnPagedOrders() {
//            Page<Order> orderPage = new PageImpl<>(List.of(pendingOrder));
//            when(orderRepository.findByUserId(eq(USER_ID), any(PageRequest.class)))
//                    .thenReturn(orderPage);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            PageResponse<OrderResponse> result = orderService.getMyOrders(USER_ID, , 10);
//
//            assertThat(result.getContent()).containsExactly(orderResponse);
//            assertThat(result.getTotalElements()).isEqualTo(1);
//        }
//
//        @Test
//        @DisplayName("không có đơn hàng nào → trả về page rỗng")
//        void givenNoOrders_shouldReturnEmptyPage() {
//            Page<Order> emptyPage = new PageImpl<>(List.of());
//            when(orderRepository.findByUserId(eq(USER_ID), any())).thenReturn(emptyPage);
//
//            PageResponse<OrderResponse> result = orderService.getMyOrders(USER_ID, 0, 10);
//
//            assertThat(result.getContent()).isEmpty();
//            assertThat(result.getTotalElements()).isZero();
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    // getAllOrders (Admin)
//    // ─────────────────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("getAllOrders()")
//    class GetAllOrders {
//
//        @Test
//        @DisplayName("status = null → lấy tất cả đơn hàng")
//        void givenNullStatus_shouldFindAll() {
//            Page<Order> orderPage = new PageImpl<>(List.of(pendingOrder));
//            when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            PageResponse<OrderResponse> result = orderService.getAllOrders(null, 0, 10);
//
//            assertThat(result.getContent()).containsExactly(orderResponse);
//            verify(orderRepository).findAll(any(PageRequest.class));
//            verify(orderRepository, never()).findByStatus(any(), any());
//        }
//
//        @Test
//        @DisplayName("status = PENDING → lọc theo status")
//        void givenStatus_shouldFilterByStatus() {
//            Page<Order> orderPage = new PageImpl<>(List.of(pendingOrder));
//            when(orderRepository.findByStatus(eq(OrderStatus.PENDING), any(PageRequest.class)))
//                    .thenReturn(orderPage);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            PageResponse<OrderResponse> result = orderService.getAllOrders(OrderStatus.PENDING, 0, 10);
//
//            assertThat(result.getContent()).containsExactly(orderResponse);
//            verify(orderRepository).findByStatus(eq(OrderStatus.PENDING), any());
//            verify(orderRepository, never()).findAll(any(PageRequest.class));
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    // updateOrderStatus (Admin)
//    // ─────────────────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("updateOrderStatus()")
//    class UpdateOrderStatus {
//
//        @ParameterizedTest(name = "{0} → {1} hợp lệ")
//        @CsvSource({
//                "PENDING,    CONFIRMED",
//                "PENDING,    CANCELLED",
//                "CONFIRMED,  PROCESSING",
//                "CONFIRMED,  CANCELLED",
//                "PROCESSING, SHIPPING",
//                "SHIPPING,   DELIVERED",
//                "DELIVERED,  RETURNED",
//                "REFUND_REQUESTED, REFUNDED",
//        })
//        @DisplayName("các chuyển trạng thái hợp lệ theo state machine")
//        void givenValidTransitions_shouldUpdateStatus(String from, String to) {
//            OrderStatus fromStatus = OrderStatus.valueOf(from);
//            OrderStatus toStatus   = OrderStatus.valueOf(to);
//            pendingOrder.setStatus(fromStatus);
//
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//            // Payment mock — có thể không tìm thấy, không ảnh hưởng flow chính
//            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
//
//            orderService.updateOrderStatus(ORDER_ID, toStatus);
//
//            assertThat(pendingOrder.getStatus()).isEqualTo(toStatus);
//            verify(orderRepository).save(pendingOrder);
//        }
//
//        @ParameterizedTest(name = "{0} → {1} không hợp lệ → BusinessException")
//        @CsvSource({
//                "PENDING,    SHIPPING",
//                "PENDING,    DELIVERED",
//                "CONFIRMED,  DELIVERED",
//                "SHIPPING,   CONFIRMED",
//                "DELIVERED,  PENDING",
//                "CANCELLED,  CONFIRMED",
//                "REFUNDED,   CONFIRMED",
//        })
//        @DisplayName("chuyển trạng thái không hợp lệ → ném BusinessException")
//        void givenInvalidTransitions_shouldThrowBusinessException(String from, String to) {
//            pendingOrder.setStatus(OrderStatus.valueOf(from));
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.updateOrderStatus(ORDER_ID, OrderStatus.valueOf(to)))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("Không thể chuyển trạng thái");
//        }
//
//        @Test
//        @DisplayName("chuyển sang DELIVERED với COD → payment tự động PAID")
//        void givenDeliveredWithCod_shouldMarkPaymentAsPaid() {
//            pendingOrder.setStatus(OrderStatus.SHIPPING);
//            pendingOrder.setPaymentMethod(PaymentMethod.COD);
//            Payment payment = Payment.builder()
//                    .id(1L).order(pendingOrder).status(PaymentStatus.UNPAID).build();
//
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            orderService.updateOrderStatus(ORDER_ID, OrderStatus.DELIVERED);
//
//            assertThat(pendingOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
//            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
//            verify(paymentRepository).save(payment);
//        }
//
//        @Test
//        @DisplayName("chuyển sang REFUNDED → payment status cũng REFUNDED")
//        void givenRefunded_shouldMarkPaymentAsRefunded() {
//            pendingOrder.setStatus(OrderStatus.REFUND_REQUESTED);
//            Payment payment = Payment.builder()
//                    .id(1L).order(pendingOrder).status(PaymentStatus.PAID).build();
//
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            orderService.updateOrderStatus(ORDER_ID, OrderStatus.REFUNDED);
//
//            assertThat(pendingOrder.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
//            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
//        }
//
//        @Test
//        @DisplayName("orderId không tồn tại → ném ResourceNotFoundException")
//        void givenMissingOrder_shouldThrow() {
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> orderService.updateOrderStatus(ORDER_ID, OrderStatus.CONFIRMED))
//                    .isInstanceOf(ResourceNotFoundException.class);
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    // cancelOrder
//    // ─────────────────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("cancelOrder()")
//    class CancelOrder {
//
//        @Test
//        @DisplayName("huỷ đơn PENDING thành công → trả về OrderResponse với CANCELLED")
//        void givenPendingOrder_shouldCancelSuccessfully() {
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            orderService.cancelOrder(ORDER_ID, USER_ID);
//
//            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
//            verify(orderRepository).save(pendingOrder);
//        }
//
//        @Test
//        @DisplayName("huỷ đơn PENDING → tồn kho được hoàn lại")
//        void givenPendingOrder_shouldRestoreStock() {
//            int originalStock = batch.getRemainingQuantity(); // 100
//            // Giả sử khi tạo đơn batch đã bị trừ 2 → còn 98; orderItem.quantity = 2
//            batch.setRemainingQuantity(98);
//
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            orderService.cancelOrder(ORDER_ID, USER_ID);
//
//            // 98 + 2 = 100
//            assertThat(batch.getRemainingQuantity()).isEqualTo(100);
//            verify(inventoryBatchRepository).save(batch);
//        }
//
//        @Test
//        @DisplayName("huỷ đơn khi batch đã SOLD_OUT → status batch trở về AVAILABLE")
//        void givenSoldOutBatch_shouldRestoreToAvailable() {
//            batch.setRemainingQuantity(0);
//            batch.setStatus(BatchStatus.SOLD_OUT);
//
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            orderService.cancelOrder(ORDER_ID, USER_ID);
//
//            assertThat(batch.getStatus()).isEqualTo(BatchStatus.AVAILABLE);
//        }
//
//        @Test
//        @DisplayName("user không phải chủ đơn hàng → ném UnauthorizedException")
//        void givenOtherUser_shouldThrowUnauthorizedException() {
//            Long otherUserId = 99L;
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, otherUserId))
//                    .isInstanceOf(UnauthorizedException.class);
//        }
//
//        @Test
//        @DisplayName("đơn hàng không ở trạng thái PENDING → ném BusinessException")
//        void givenNonPendingOrder_shouldThrowBusinessException() {
//            pendingOrder.setStatus(OrderStatus.CONFIRMED);
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("PENDING");
//        }
//
//        @Test
//        @DisplayName("đơn đã thanh toán online → không cho huỷ, ném BusinessException")
//        void givenOnlinePaidOrder_shouldThrowBusinessException() {
//            pendingOrder.setPaymentStatus(PaymentStatus.PAID);
//            pendingOrder.setPaymentMethod(PaymentMethod.VNPAY);
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("đã được thanh toán");
//        }
//
//        @Test
//        @DisplayName("đơn COD đã thanh toán vẫn cho phép huỷ")
//        void givenCodPaidOrder_shouldAllowCancel() {
//            pendingOrder.setPaymentStatus(PaymentStatus.PAID);
//            pendingOrder.setPaymentMethod(PaymentMethod.COD);
//
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            orderService.cancelOrder(ORDER_ID, USER_ID);
//
//            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────
//    // requestRefund
//    // ─────────────────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("requestRefund()")
//    class RequestRefund {
//
//        @BeforeEach
//        void setUpPaidOrder() {
//            // Cần đơn đã thanh toán, trạng thái phù hợp (CONFIRMED)
//            pendingOrder.setStatus(OrderStatus.CONFIRMED);
//            pendingOrder.setPaymentStatus(PaymentStatus.PAID);
//        }
//
//        @Test
//        @DisplayName("yêu cầu hoàn tiền hợp lệ → status chuyển REFUND_REQUESTED")
//        void givenValidPaidOrder_shouldSetRefundRequestedStatus() {
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            orderService.requestRefund(ORDER_ID, USER_ID);
//
//            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
//            verify(orderRepository).save(pendingOrder);
//        }
//
//        @Test
//        @DisplayName("yêu cầu hoàn tiền → tồn kho được hoàn lại")
//        void givenRefundRequest_shouldRestoreStock() {
//            batch.setRemainingQuantity(98);
//
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//            when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
//            when(orderMapper.toResponse(pendingOrder)).thenReturn(orderResponse);
//
//            orderService.requestRefund(ORDER_ID, USER_ID);
//
//            assertThat(batch.getRemainingQuantity()).isEqualTo(100); // 98 + 2
//        }
//
//        @Test
//        @DisplayName("user không phải chủ đơn → ném UnauthorizedException")
//        void givenOtherUser_shouldThrowUnauthorizedException() {
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.requestRefund(ORDER_ID, 99L))
//                    .isInstanceOf(UnauthorizedException.class);
//        }
//
//        @Test
//        @DisplayName("đơn chưa thanh toán → ném BusinessException")
//        void givenUnpaidOrder_shouldThrowBusinessException() {
//            pendingOrder.setPaymentStatus(PaymentStatus.UNPAID);
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.requestRefund(ORDER_ID, USER_ID))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("chưa được thanh toán");
//        }
//
//        @Test
//        @DisplayName("đơn đang SHIPPING → không cho hoàn tiền, ném BusinessException")
//        void givenShippingOrder_shouldThrowBusinessException() {
//            pendingOrder.setStatus(OrderStatus.SHIPPING);
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.requestRefund(ORDER_ID, USER_ID))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("đang được giao");
//        }
//
//        @Test
//        @DisplayName("đơn đã DELIVERED → không cho hoàn tiền, ném BusinessException")
//        void givenDeliveredOrder_shouldThrowBusinessException() {
//            pendingOrder.setStatus(OrderStatus.DELIVERED);
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.requestRefund(ORDER_ID, USER_ID))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("Trả Hàng");
//        }
//
//        @Test
//        @DisplayName("đã yêu cầu hoàn tiền trước đó (REFUND_REQUESTED) → ném BusinessException")
//        void givenAlreadyRefundRequested_shouldThrowBusinessException() {
//            pendingOrder.setStatus(OrderStatus.REFUND_REQUESTED);
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.requestRefund(ORDER_ID, USER_ID))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessageContaining("đã được yêu cầu hoàn tiền");
//        }
//
//        @Test
//        @DisplayName("đã REFUNDED rồi → ném BusinessException")
//        void givenAlreadyRefunded_shouldThrowBusinessException() {
//            pendingOrder.setStatus(OrderStatus.REFUNDED);
//            when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
//
//            assertThatThrownBy(() -> orderService.requestRefund(ORDER_ID, USER_ID))
//                    .isInstanceOf(BusinessException.class);
//        }
//    }
//}