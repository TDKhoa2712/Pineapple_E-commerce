package backend.pineapple_ecommerce.modules.payment.service;

import backend.pineapple_ecommerce.modules.payment.dto.response.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Quản lý thanh toán.
 *
 * <p>Luồng VNPay chuẩn:
 * <ol>
 *   <li>{@link #initiatePayment} — sinh URL chuyển hướng tới VNPay</li>
 *   <li>{@link #handleVnpayIpn} — IPN server-to-server: verify chữ ký,
 *       kiểm tra idempotency, cập nhật DB, publish email event</li>
 *   <li>{@link #buildReturnRedirectUrl} — Return URL: verify chữ ký rồi
 *       redirect FE, không ghi DB</li>
 * </ol>
 *
 * <p><strong>Lưu ý thiết kế Return URL:</strong><br>
 * Nhận {@link HttpServletRequest} thay vì (responseCode, txnRef) raw string
 * để service tự verify chữ ký trước khi đọc params — tránh attacker forge
 * query string và khiến FE hiển thị trạng thái sai.
 */
public interface PaymentService {

    /**
     * Khởi tạo thanh toán cho đơn hàng.
     * - COD  : tạo Payment record UNPAID.
     * - VNPAY: sinh redirect URL tới cổng thanh toán.
     */
    PaymentResponse initiatePayment(Long orderId, Long userId, HttpServletRequest request);

    /**
     * Xử lý IPN webhook từ VNPay (server-to-server).
     * Đây là nơi DUY NHẤT cập nhật trạng thái Payment/Order vào DB.
     * Luôn trả HTTP 200 với body {"RspCode": "xx", "Message": "..."}.
     */
    Map<String, String> handleVnpayIpn(HttpServletRequest request);

    /**
     * Xây dựng URL redirect về Frontend sau khi VNPay redirect trình duyệt.
     *
     * <p>Nhận toàn bộ request để verify chữ ký VNPay trước khi đọc params.
     * Nếu chữ ký không hợp lệ → redirect với status=invalid thay vì success/failed.
     * DB không được cập nhật ở đây — FE tự gọi GET /payments/order/{id}.
     *
     * @param request HttpServletRequest chứa toàn bộ query params từ VNPay
     * @return URL redirect về FE (với status, txnRef, code)
     */
    String buildReturnRedirectUrl(HttpServletRequest request);

    /**
     * COD: Admin xác nhận đã thu tiền mặt khi giao hàng thành công.
     */
    PaymentResponse confirmCodPayment(Long orderId);

    /** Lấy thông tin thanh toán của một đơn hàng. */
    PaymentResponse getPaymentByOrderId(Long orderId, Long userId);
}