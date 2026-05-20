package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.response.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Quản lý thanh toán.
 *
 * <p>Luồng VNPAY chuẩn:
 * <ol>
 *   <li>{@link #initiatePayment} — sinh URL chuyển hướng tới VNPAY</li>
 *   <li>{@link #handleVnpayIpn} — IPN server-to-server: xác minh chữ ký,
 *       kiểm tra idempotency, cập nhật DB, publish email event</li>
 *   <li>{@link #buildReturnRedirectUrl} — Return URL: chỉ redirect FE,
 *       không ghi DB</li>
 * </ol>
 */
public interface PaymentService {

    /**
     * Khởi tạo thanh toán cho đơn hàng.
     * - COD  : tạo Payment record UNPAID.
     * - VNPAY: sinh redirect URL tới cổng thanh toán.
     */
    PaymentResponse initiatePayment(Long orderId, Long userId, HttpServletRequest request);

//    Map<String, String> handleVnpayIpn(Map<String, String> params);

    Map<String, String> handleVnpayIpn(HttpServletRequest request);

    String buildReturnRedirectUrl(String vnpResponseCode, String txnRef);

    /**
     * COD: Admin xác nhận đã thu tiền mặt khi giao hàng thành công.
     */
    PaymentResponse confirmCodPayment(Long orderId);

    /** Lấy thông tin thanh toán của một đơn hàng. */
    PaymentResponse getPaymentByOrderId(Long orderId);
}