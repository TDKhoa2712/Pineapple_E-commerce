package backend.pineapple_ecommerce.modules.auth.service;

import backend.pineapple_ecommerce.common.exception.BusinessException;

/**
 * Contract cho luồng xác thực email (Email Verification).
 *
 * <p>Flow tổng quan:
 * <ol>
 *   <li>Khi đăng ký LOCAL account → tự động gọi {@link #sendVerificationOtp}</li>
 *   <li>Người dùng nhập OTP → gọi {@link #verifyEmail}</li>
 *   <li>Nếu không nhận được → gọi {@link #resendVerificationOtp} (rate limited)</li>
 * </ol>
 */
public interface EmailVerificationService {

    /**
     * Tạo OTP xác thực email và gửi cho người dùng.
     * Xoá OTP cũ (nếu có) trước khi tạo mới — đảm bảo 1 OTP active tại 1 thời điểm.
     *
     * <p>Được gọi nội bộ sau khi register() thành công.
     *
     * @param email email của user vừa đăng ký
     */
    void sendVerificationOtp(String email);

    /**
     * Xác thực OTP và đánh dấu email đã xác minh.
     * Sau khi thành công: {@code user.emailVerified = true}, OTP bị đánh dấu used.
     *
     * @param email email cần xác thực
     * @param otp   mã OTP 6 chữ số
     * @throws BusinessException nếu OTP sai/hết hạn
     */
    void verifyEmail(String email, String otp);

    /**
     * Gửi lại OTP xác thực email theo yêu cầu của người dùng.
     *
     * <p>Rate limit: tối đa 3 lần trong 10 phút.
     * Nếu vượt quá → ném {@link BusinessException}.
     *
     * @param email email cần gửi lại OTP
     * @throws BusinessException nếu vượt rate limit
     *         hoặc email không tồn tại / đã xác thực rồi
     */
    void resendVerificationOtp(String email);
}