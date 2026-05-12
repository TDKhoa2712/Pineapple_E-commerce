package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.entity.RefreshToken;
import backend.pineapple_ecommerce.entity.User;

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
