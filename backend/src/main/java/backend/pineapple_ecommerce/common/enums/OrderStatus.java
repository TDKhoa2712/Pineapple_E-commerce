package backend.pineapple_ecommerce.common.enums;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {

    PENDING,          // Đơn mới tạo
    CONFIRMED,        // Admin xác nhận đơn
    PROCESSING,       // Đang chuẩn bị hàng
    SHIPPING,         // Đang giao hàng
    DELIVERED,        // Giao thành công
    CANCELLED,        // Huỷ đơn
    REFUND_REQUESTED, // Khách yêu cầu hoàn tiền
    REFUNDED,         // Đã hoàn tiền
    RETURNED;         // Trả hàng (sau khi nhận)

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(PENDING,          Set.of(CONFIRMED, CANCELLED)),
            Map.entry(CONFIRMED,        Set.of(PROCESSING, CANCELLED)),
            Map.entry(PROCESSING,       Set.of(SHIPPING, CANCELLED)),
            Map.entry(SHIPPING,         Set.of(DELIVERED, CANCELLED, RETURNED)),  // Có thể huỷ hoặc hoàn trả khi đang ship
            Map.entry(DELIVERED,        Set.of(REFUND_REQUESTED, RETURNED)),
            Map.entry(REFUND_REQUESTED, Set.of(REFUNDED, RETURNED)),    // Không cho quay lại DELIVERED
            Map.entry(REFUNDED,         Set.of()),                      // Kết thúc
            Map.entry(RETURNED,         Set.of()),                      // Kết thúc
            Map.entry(CANCELLED,        Set.of(REFUND_REQUESTED, REFUNDED))                       // Cho phép hoàn tiền sau khi hủy
    );

    /**
     * Kiểm tra xem trạng thái hiện tại có thể chuyển sang trạng thái mới không.
     */
    public boolean canTransitionTo(OrderStatus next) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }

    /**
     * Helper method tiện lợi cho logging hoặc validation
     */
    public boolean isTerminalState() {
        return this == CANCELLED || this == REFUNDED || this == RETURNED;
    }

    public boolean isActive() {
        return !isTerminalState();
    }
}