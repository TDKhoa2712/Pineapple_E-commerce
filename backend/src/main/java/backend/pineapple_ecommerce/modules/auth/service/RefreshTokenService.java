package backend.pineapple_ecommerce.modules.auth.service;

import backend.pineapple_ecommerce.modules.auth.models.RefreshToken;
import backend.pineapple_ecommerce.modules.user.models.User;

public interface RefreshTokenService {

    /**
     * Tạo (hoặc thay thế) refresh token cho user.
     */

   RefreshToken createRefreshToken(User user);

    /**
     * Kiểm tra và trả về RefreshToken hợp lệ.
     * Xoá token nếu đã hết hạn (rotation strategy).
     */

    RefreshToken verifyRefreshToken(String tokenValue);

    /**
     * Thu hồi refresh token (dùng khi logout).
     */

    void revokeByUserId(Long userId);
}
