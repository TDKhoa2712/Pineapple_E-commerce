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

    void sendWelcomeEmail(String toEmail, String fullName);

    void sendPasswordResetOtp(String toEmail, String otp);

    /**
     * Gửi OTP xác thực email cho người dùng vừa đăng ký.
     *
     * @param toEmail địa chỉ email cần xác thực
     * @param otp     mã OTP 6 chữ số
     */
    void sendEmailVerificationOtp(String toEmail, String otp);

    void sendOrderConfirmation(String toEmail, OrderResponse order);

    void sendOrderStatusUpdate(String toEmail, OrderResponse order, String newStatus);

    void sendFarmApprovalResult(String toEmail, String farmName,
                                boolean approved, String rejectionReason);
}
