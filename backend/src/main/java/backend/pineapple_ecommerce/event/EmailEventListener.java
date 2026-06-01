package backend.pineapple_ecommerce.event;

import backend.pineapple_ecommerce.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener nhận domain events và gọi EmailService SAU KHI transaction commit.
 *
 * <p>phase = AFTER_COMMIT đảm bảo:
 * <ul>
 *   <li>Email chỉ gửi khi data đã được persist thành công</li>
 *   <li>Nếu transaction rollback (lỗi DB, exception), email không được gửi</li>
 *   <li>Tách biệt hoàn toàn logic email khỏi transaction context</li>
 * </ul>
 *
 * <p>Kết hợp với @Async trong EmailServiceImpl:
 * AFTER_COMMIT → listener chạy trên tx thread → gọi @Async → email thực sự
 * chạy trên emailTaskExecutor thread pool → request thread không bị block.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    // ─────────────────────────────────────────────
    // USER REGISTERED
    // ─────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(EmailEvents.UserRegisteredEvent event) {
        log.debug("[EmailEvent] UserRegistered → email={}", event.getEmail());
        emailService.sendWelcomeEmail(event.getEmail(), event.getFullName());
    }

    // ─────────────────────────────────────────────
    // ORDER CREATED
    // ─────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(EmailEvents.OrderCreatedEvent event) {
        log.debug("[EmailEvent] OrderCreated → email={}, orderId={}",
                  event.getToEmail(), event.getOrder().getId());
        emailService.sendOrderConfirmation(event.getToEmail(), event.getOrder());
    }

    // ─────────────────────────────────────────────
    // ORDER STATUS CHANGED
    // ─────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(EmailEvents.OrderStatusChangedEvent event) {
        log.debug("[EmailEvent] OrderStatusChanged → email={}, orderId={}, status={}",
                  event.getToEmail(), event.getOrder().getId(), event.getNewStatusLabel());
        emailService.sendOrderStatusUpdate(
                event.getToEmail(), event.getOrder(), event.getNewStatusLabel());
    }

    // ─────────────────────────────────────────────
    // FARM APPROVAL
    // ─────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFarmApproval(EmailEvents.FarmApprovalEvent event) {
        log.debug("[EmailEvent] FarmApproval → email={}, farm='{}', approved={}",
                  event.getOwnerEmail(), event.getFarmName(), event.isApproved());
        emailService.sendFarmApprovalResult(
                event.getOwnerEmail(),
                event.getFarmName(),
                event.isApproved(),
                event.getRejectionReason());
    }
}
