package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.LoginRequest;
import backend.pineapple_ecommerce.dto.request.RefreshTokenRequest;
import backend.pineapple_ecommerce.dto.request.RegisterRequest;
import backend.pineapple_ecommerce.dto.response.AuthResponse;

/**
 * Xử lý toàn bộ nghiệp vụ xác thực: đăng ký, đăng nhập, refresh token, đăng xuất.
 *
 * THAY ĐỔI: logout(String refreshToken) — nhận refreshToken thay vì không tham số,
 * để service có thể tự tra cứu userId và thu hồi token mà không cần inject SecurityContext.
 */
public interface AuthService {

    /**
     * Đăng ký tài khoản mới.
     * Kiểm tra email/phone trùng, mã hoá mật khẩu, gán role USER, khởi tạo Cart.
     * Trả về JWT ngay sau khi đăng ký thành công.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Đăng nhập bằng email + password.
     * Trả về accessToken (short-lived) + refreshToken (long-lived).
     */
    AuthResponse login(LoginRequest request);

    /**
     * Làm mới accessToken bằng refreshToken hợp lệ.
     * Refresh token sẽ được rotate (tạo mới) sau mỗi lần gọi.
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Đăng xuất: vô hiệu hoá refreshToken khỏi DB.
     *
     * @param refreshToken chuỗi refresh token cần thu hồi
     */
    void logout(String refreshToken);
}
