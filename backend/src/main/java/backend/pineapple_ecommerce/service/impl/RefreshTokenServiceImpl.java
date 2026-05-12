package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.config.JwtProperties;
import backend.pineapple_ecommerce.entity.RefreshToken;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.repository.RefreshTokenRepository;
import backend.pineapple_ecommerce.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Quản lý vòng đời Refresh Token:
 * - Mỗi user chỉ có 1 refresh token tại 1 thời điểm (upsert)
 * - Token được lưu DB dưới dạng UUID opaque string
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /**
     * Tạo (hoặc thay thế) refresh token cho user.
     */
    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = refreshTokenRepository.findByUserId(user.getId())
                .orElse(RefreshToken.builder().user(user).build());

        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()));

        return refreshTokenRepository.save(token);
    }

    /**
     * Kiểm tra và trả về RefreshToken hợp lệ.
     * Xoá token nếu đã hết hạn (rotation strategy).
     */
    @Override
    @Transactional
    public RefreshToken verifyRefreshToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException("Refresh token không hợp lệ hoặc đã bị thu hồi"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException("Refresh token đã hết hạn. Vui lòng đăng nhập lại");
        }

        refreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        return refreshToken;
    }

    /**
     * Thu hồi refresh token (dùng khi logout).
     */
    @Override
    @Transactional
    public void revokeByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
