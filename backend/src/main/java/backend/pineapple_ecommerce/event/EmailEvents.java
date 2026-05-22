package backend.pineapple_ecommerce.event;

import backend.pineapple_ecommerce.modules.order.dto.response.OrderResponse;
import lombok.Getter;

/**
 * Domain events cho hệ thống email.
 *
 * <p>Tại sao dùng Event thay vì gọi emailService trực tiếp trong @Transactional method?
 *
 * <p>Vấn đề khi gọi trực tiếp:
 * - @Async chạy trên thread khác, nhưng thread đó có thể bắt đầu
 *   TRƯỚC khi transaction của thread gốc commit.
 * - Kết quả: email gửi đi nhưng data có thể chưa/không bao giờ persist
 *   (nếu transaction rollback sau đó).
 *
 * <p>Giải pháp: publish event → listener với phase = AFTER_COMMIT
 * đảm bảo email chỉ gửi khi transaction đã commit thành công.
 *
 * <p>Cách dùng trong service:
 * <pre>
 *   &#64;Autowired
 *   private ApplicationEventPublisher eventPublisher;
 *
 *   // Sau khi save entity thành công:
 *   eventPublisher.publishEvent(new EmailEvents.OrderCreatedEvent(toEmail, orderResponse));
 * </pre>
 */
public final class EmailEvents {

    private EmailEvents() {} // utility class — không instantiate

    // ─────────────────────────────────────────────
    // USER REGISTERED
    // ─────────────────────────────────────────────

    @Getter
    public static class UserRegisteredEvent {
        private final String email;
        private final String fullName;

        public UserRegisteredEvent(String email, String fullName) {
            this.email    = email;
            this.fullName = fullName;
        }
    }

    // ─────────────────────────────────────────────
    // ORDER CREATED
    // ─────────────────────────────────────────────

    @Getter
    public static class OrderCreatedEvent {
        private final String        toEmail;
        private final OrderResponse order;

        public OrderCreatedEvent(String toEmail, OrderResponse order) {
            this.toEmail = toEmail;
            this.order   = order;
        }
    }

    // ─────────────────────────────────────────────
    // ORDER STATUS CHANGED
    // ─────────────────────────────────────────────

    @Getter
    public static class OrderStatusChangedEvent {
        private final String        toEmail;
        private final OrderResponse order;
        private final String        newStatusLabel; // tiếng Việt, ví dụ "Đang giao hàng"

        public OrderStatusChangedEvent(String toEmail, OrderResponse order, String newStatusLabel) {
            this.toEmail        = toEmail;
            this.order          = order;
            this.newStatusLabel = newStatusLabel;
        }
    }

    // ─────────────────────────────────────────────
    // FARM APPROVAL
    // ─────────────────────────────────────────────

    @Getter
    public static class FarmApprovalEvent {
        private final String  ownerEmail;
        private final String  farmName;
        private final boolean approved;
        private final String  rejectionReason;

        public FarmApprovalEvent(String ownerEmail, String farmName,
                                 boolean approved, String rejectionReason) {
            this.ownerEmail      = ownerEmail;
            this.farmName        = farmName;
            this.approved        = approved;
            this.rejectionReason = rejectionReason;
        }
    }
}
