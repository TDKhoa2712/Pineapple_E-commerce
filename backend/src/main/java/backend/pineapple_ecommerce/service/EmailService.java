package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.response.OrderResponse;

/**
 * Contract cho hệ thống gửi email.
 *
 * <p>Mỗi method đại diện cho một loại email nghiệp vụ.
 * Implementation (@Async) đảm bảo không block request thread.
 *
 * <p>Các method được gọi sau khi transaction COMMIT
 * thông qua @TransactionalEventListener — xem EmailEventListener.
 */
public interface EmailService {

    /**
     * Gửi email chào mừng sau khi đăng ký thành công.
     *
     * @param toEmail  địa chỉ email người dùng
     * @param fullName họ tên đầy đủ
     */
    void sendWelcomeEmail(String toEmail, String fullName);

    /**
     * Gửi OTP reset mật khẩu.
     *
     * @param toEmail địa chỉ email
     * @param otp     mã OTP 6 chữ số
     */
    void sendPasswordResetOtp(String toEmail, String otp);

    /**
     * Gửi xác nhận đơn hàng sau khi tạo thành công.
     *
     * @param toEmail địa chỉ email người đặt hàng
     * @param order   thông tin đơn hàng (DTO đã được map)
     */
    void sendOrderConfirmation(String toEmail, OrderResponse order);

    /**
     * Gửi thông báo cập nhật trạng thái đơn hàng.
     * Áp dụng cho: CONFIRMED, SHIPPING, DELIVERED, CANCELLED.
     *
     * @param toEmail   địa chỉ email người đặt hàng
     * @param order     thông tin đơn hàng
     * @param newStatus trạng thái mới (dạng chuỗi tiếng Việt để hiển thị)
     */
    void sendOrderStatusUpdate(String toEmail, OrderResponse order, String newStatus);

    /**
     * Gửi kết quả duyệt / từ chối farm.
     *
     * @param toEmail         địa chỉ email chủ farm
     * @param farmName        tên farm
     * @param approved        true = duyệt, false = từ chối
     * @param rejectionReason lý do từ chối (null nếu được duyệt)
     */
    void sendFarmApprovalResult(String toEmail, String farmName,
                                boolean approved, String rejectionReason);
}
