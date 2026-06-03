package backend.pineapple_ecommerce.infrastructure.scheduler;

import backend.pineapple_ecommerce.modules.auth.repository.RefreshTokenRepository;
import backend.pineapple_ecommerce.modules.auth.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;

    /**
     * Chạy lúc 2:00 AM mỗi ngày, xóa tất cả token và OTP đã hết hạn hoặc đã dùng.
     * Cron: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        int deletedRefresh = refreshTokenRepository.deleteAllExpiredTokens(Instant.now());
        log.info("Token cleanup: deleted {} expired refresh tokens", deletedRefresh);

        int deletedOtp = otpTokenRepository.deleteExpiredAndUsed(LocalDateTime.now());
        log.info("Token cleanup: deleted {} expired or used OTP tokens", deletedOtp);
    }
}