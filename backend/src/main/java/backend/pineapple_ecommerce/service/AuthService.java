package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.LoginRequest;
import backend.pineapple_ecommerce.dto.request.RefreshTokenRequest;
import backend.pineapple_ecommerce.dto.request.RegisterRequest;
import backend.pineapple_ecommerce.dto.response.AuthResponse;

/**
 * Xử lý toàn bộ nghiệp vụ xác thực: đăng ký, đăng nhập, refresh token, đăng xuất.
 */
public interface AuthService {

    /**
     * Đăng ký tài khoản mới.
     * Kiểm tra email/phone trùng, mã hoá mật khẩu, gán role USER, khởi tạo Cart.
     * Gửi OTP xác thực email. KHÔNG trả JWT ngay.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Đăng nhập bằng email + password.
     * LOCAL user phải có emailVerified = true mới được cấp JWT.
     * OAuth2 user (Google/FB) bỏ qua kiểm tra này.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Cấp JWT cho user sau khi xác thực OTP thành công (không cần password).
     * Chỉ được gọi từ {@code AuthController.verifyEmail()} sau khi OTP đã được validate.
     *
     * @param email email của user đã vừa verify OTP thành công
     */
    AuthResponse loginAfterVerification(String email);

    /**
     * Làm mới accessToken bằng refreshToken hợp lệ.
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Đăng xuất — thu hồi refreshToken.
     */
    void logout(String refreshToken);
}
