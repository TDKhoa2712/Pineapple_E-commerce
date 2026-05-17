package backend.pineapple_ecommerce.service;

/**
 * Service xử lý đặt lại mật khẩu qua OTP.
 */
public interface PasswordResetService {

    /**
     * Gửi OTP về email (không tiết lộ email tồn tại hay không).
     */
    void initiateReset(String email);

    /**
     * Xác thực OTP và đổi mật khẩu mới.
     */
    void resetPassword(String email, String otp, String newPassword);
}