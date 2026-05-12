package backend.pineapple_ecommerce.scheduler;

import backend.pineapple_ecommerce.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Chạy lúc 2:00 AM mỗi ngày, xóa tất cả token đã hết hạn.
     * Cron: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpiredTokens(Instant.now());
        log.info("Token cleanup: deleted {} expired refresh tokens", deleted);
    }
}