package backend.pineapple_ecommerce.event;

import lombok.Getter;

/**
 * Sự kiện phát ra khi một đơn hàng bị hủy.
 */
@Getter
public class OrderCancelledEvent {
    private final Long orderId;

    public OrderCancelledEvent(Long orderId) {
        this.orderId = orderId;
    }
}
