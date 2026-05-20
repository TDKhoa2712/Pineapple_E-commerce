package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.response.VNPayQueryResult;
import backend.pineapple_ecommerce.entity.Order;
import backend.pineapple_ecommerce.entity.Payment;
import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.enums.PaymentStatus;
import backend.pineapple_ecommerce.event.EmailEvents;
import backend.pineapple_ecommerce.mapper.OrderMapper;
import backend.pineapple_ecommerce.repository.OrderRepository;
import backend.pineapple_ecommerce.repository.PaymentRepository;
import backend.pineapple_ecommerce.service.VNPayQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconcileProcessor {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final VNPayQueryService vnpayQueryService;

    // Bổ sung dependencies để phục vụ việc gửi Mail Event
    private final ApplicationEventPublisher eventPublisher;
    private final OrderMapper orderMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSinglePayment(String txnRef) {
        // 1. Lấy dữ liệu và khóa row lại
        Payment payment = paymentRepository.findByTransactionCodeForUpdate(txnRef)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch: " + txnRef));
        if (payment.getReconcileCount() >= 5) {
            log.warn("[Processor] Giao dịch {} đã retry {} lần, bỏ qua.",
                    txnRef, payment.getReconcileCount());
            return;
        }
        payment.setReconcileCount(payment.getReconcileCount() + 1);

        // 2. Double check idempotency
        if (payment.getStatus() != PaymentStatus.UNPAID) {
            log.info("[Processor] Giao dịch {} đã được xử lý (trạng thái: {}). Bỏ qua.", txnRef, payment.getStatus());
            return;
        }

        // 3. Gọi API sang VNPay để hỏi kết quả thực sự
        VNPayQueryResult result = vnpayQueryService.queryTransaction(payment);
        Order order = payment.getOrder();

        // Lưu lại phản hồi nguyên bản từ VNPay QueryDR
        payment.setRawResponse("QUERY_DR: " + result.getRawResponse());

        // 4. Xử lý kết quả
        if ("00".equals(result.getTransactionStatus())) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);

            log.info("[Processor] Đồng bộ thành công giao dịch bị kẹt: {}", txnRef);

            // Thực thi gửi Email thông qua Event
            publishPaymentSuccessEmail(order);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.CANCELLED);
            log.warn("[Processor] Giao dịch thất bại từ VNPay: {}", txnRef);
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
    }

    /**
     * Tách riêng hàm publish event để code gọn gàng, tái sử dụng logic giống IPN.
     */
    private void publishPaymentSuccessEmail(Order order) {
        try {
            String toEmail = order.getUser().getEmail();
            eventPublisher.publishEvent(
                    new EmailEvents.OrderStatusChangedEvent(
                            toEmail,
                            orderMapper.toResponse(order),
                            "Đã thanh toán - Đang xác nhận"
                    )
            );
            log.info("[Processor] Đã publish event gửi email cho đơn hàng {}", order.getId());
        } catch (Exception ex) {
            log.error("[Processor] Không thể publish email event — orderId={}: {}",
                    order.getId(), ex.getMessage());
        }
    }
}