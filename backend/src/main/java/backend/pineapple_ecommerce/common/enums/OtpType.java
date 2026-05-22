package backend.pineapple_ecommerce.common.enums;

/**
 * Phân loại mục đích sử dụng OTP.
 *
 * <p>Tách biệt OTP theo type giúp:
 * <ul>
 *   <li>Tránh dùng lẫn OTP giữa các flow (security)</li>
 *   <li>Query DB chính xác hơn (chỉ lấy OTP đúng type)</li>
 *   <li>Dễ mở rộng thêm loại OTP mới mà không ảnh hưởng code hiện tại</li>
 * </ul>
 */
public enum OtpType {

    /** OTP dùng để đặt lại mật khẩu (Password Reset flow) */
    PASSWORD_RESET,

    /** OTP dùng để xác thực địa chỉ email khi đăng ký (Email Verification flow) */
    EMAIL_VERIFICATION
}