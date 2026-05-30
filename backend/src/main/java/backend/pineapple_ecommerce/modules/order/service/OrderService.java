package backend.pineapple_ecommerce.modules.order.service;

import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.modules.order.dto.request.BulkOrderStatusRequest;
import backend.pineapple_ecommerce.modules.order.dto.request.CreateOrderRequest;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.common.enums.PaymentMethod;

import java.time.LocalDateTime;

/**
 * Quản lý vòng đời đơn hàng: tạo, xem, cập nhật trạng thái, huỷ.
 *
 * Luồng trạng thái chuẩn:
 * PENDING → CONFIRMED → PROCESSING → SHIPPING → DELIVERED
 *        ↘ CANCELLED (user huỷ khi PENDING)
 *                                              ↘ RETURNED
 * DELIVERED → REFUND_REQUESTED → REFUNDED
 */
public interface OrderService {

    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    OrderResponse getOrderById(Long orderId, Long userId);

    /** Admin: xem chi tiết đơn hàng bất kỳ (không kiểm tra ownership). */
    OrderResponse getOrderByIdAsAdmin(Long orderId);

    /**
     * Lịch sử đơn hàng của user — phân trang.
     * NEW: thêm filter status (nullable = tất cả).
     */
    PageResponse<OrderResponse> getMyOrders(Long userId, OrderStatus status, int page, int size);

    /** Compat: gọi getMyOrders với status=null */
    default PageResponse<OrderResponse> getMyOrders(Long userId, int page, int size) {
        return getMyOrders(userId, null, page, size);
    }

    /**
     * Admin: lấy tất cả đơn hàng với nhiều filter kết hợp.
     * NEW: thêm userId, paymentMethod, from, to.
     */
    PageResponse<OrderResponse> getAllOrders(
            OrderStatus status,
            Long userId,
            PaymentMethod paymentMethod,
            LocalDateTime from,
            LocalDateTime to,
            String sortBy,
            String sortDirection,
            int page, int size);

    /** Compat: gọi getAllOrders với chỉ status filter */
    default PageResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size) {
        return getAllOrders(status, null, null, null, null, null, null, page, size);
    }

    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus);

    OrderResponse cancelOrder(Long orderId, Long userId);

    OrderResponse requestRefund(Long orderId, Long userId);
}
