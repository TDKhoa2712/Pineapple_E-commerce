package backend.pineapple_ecommerce.scheduler;

import backend.pineapple_ecommerce.entity.Payment;
import backend.pineapple_ecommerce.repository.PaymentRepository;
import backend.pineapple_ecommerce.service.impl.PaymentReconcileProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final PaymentReconcileProcessor processor;

    // Chạy mỗi 5 phút (300,000 ms)
    @Scheduled(fixedDelay = 300_000)
    public void reconcilePendingPayments() {
        // Quét các đơn hàng đã tạo quá 20 phút nhưng vẫn UNPAID
        // VNPay timeout sau 15 phút, job cũng query sau 15 phút. Nếu job chạy đúng lúc user vừa thanh toán xong phút 14,
        // IPN chưa kịp về — job query VNPay sớm, trạng thái trả về có thể chưa final, dẫn đến đơn bị ghi sai là FAILED.
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(20);

        List<Payment> pendingPayments = paymentRepository
                .findByStatusUnpaidAndCreatedAtBefore(cutoff);

        if (pendingPayments.isEmpty()) return;

        log.info("[Job] Bắt đầu đồng bộ {} giao dịch bị kẹt...", pendingPayments.size());

        for (Payment payment : pendingPayments) {
            try {
                processor.processSinglePayment(payment.getTransactionCode());
            } catch (Exception e) {
                log.error("[Job] Lỗi khi xử lý giao dịch {}: {}", payment.getTransactionCode(), e.getMessage());
            }
        }
    }
}