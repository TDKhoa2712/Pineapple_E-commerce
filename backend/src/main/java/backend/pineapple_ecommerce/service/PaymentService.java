package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.response.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.net.http.HttpRequest;

/**
 * Quản lý thanh toán.
 * Thiết kế mở rộng: dễ plug-in thêm provider mới (VNPay, MoMo, ...).
 */
public interface PaymentService {

    /**
     * Khởi tạo thanh toán cho đơn hàng.
     * - COD: tạo Payment record với status UNPAID, trả về thông tin đơn.
     * - VNPAY/MOMO: sinh redirect URL tới cổng thanh toán.
     */
    PaymentResponse initiatePayment(Long orderId, Long userId, HttpServletRequest request);

    /**
     * Xử lý callback/webhook từ cổng thanh toán.
     * Verify chữ ký, cập nhật PaymentStatus, cập nhật OrderStatus tương ứng.
     *
     * @param provider  tên provider (vnpay, momo, ...)
     * @param params    tất cả query params / body từ callback
     */
    void handlePaymentCallback(String provider, java.util.Map<String, String> params);

    /**
     * COD: Admin xác nhận đã thu tiền mặt khi giao hàng thành công.
     */
    PaymentResponse confirmCodPayment(Long orderId);

    /** Lấy thông tin thanh toán của một đơn hàng. */
    PaymentResponse getPaymentByOrderId(Long orderId);
}
