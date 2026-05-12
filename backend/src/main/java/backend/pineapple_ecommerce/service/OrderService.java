package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.CreateOrderRequest;
import backend.pineapple_ecommerce.dto.response.OrderResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.enums.OrderStatus;

/**
 * Quản lý vòng đời đơn hàng: tạo, xem, cập nhật trạng thái, huỷ.
 *
 * <p>Luồng trạng thái chuẩn:</p>
 * <pre>
 * PENDING → CONFIRMED → PROCESSING → SHIPPING → DELIVERED
 *        ↘ CANCELLED (user có thể huỷ khi còn PENDING)
 *                                              ↘ RETURNED
 * </pre>
 */
public interface OrderService {

    /**
     * Tạo đơn hàng từ giỏ hàng hiện tại.
     * <ul>
     *   <li>Validate giỏ không rỗng.</li>
     *   <li>Trừ tồn kho theo FIFO batch (hết hạn trước xuất trước).</li>
     *   <li>Snapshot địa chỉ giao hàng.</li>
     *   <li>Tính subtotal, shippingFee, totalAmount.</li>
     *   <li>Xoá cart sau khi tạo đơn thành công.</li>
     * </ul>
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    /** Lấy chi tiết đơn hàng. Kiểm tra quyền sở hữu (user chỉ xem được đơn của mình). */
    OrderResponse getOrderById(Long orderId, Long userId);

    /** Lịch sử đơn hàng của user hiện tại — phân trang. */
    PageResponse<OrderResponse> getMyOrders(Long userId, int page, int size);

    /** Tất cả đơn hàng — Admin, có thể lọc theo status. */
    PageResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size);

    /**
     * Admin cập nhật trạng thái đơn hàng.
     * Ném BusinessException nếu chuyển trạng thái không hợp lệ.
     */
    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus);

    /**
     * User tự huỷ đơn hàng.
     * Chỉ cho phép huỷ khi status = PENDING.
     * Hoàn lại tồn kho đã trừ.
     */
    OrderResponse cancelOrder(Long orderId, Long userId);

    /**
     * User yêu cầu hoàn tiền cho đơn hàng đã thanh toán online.
     * Trạng thái đơn hàng sẽ chuyển sang REFUND_REQUESTED.
     */
    OrderResponse requestRefund(Long orderId, Long userId);
}
