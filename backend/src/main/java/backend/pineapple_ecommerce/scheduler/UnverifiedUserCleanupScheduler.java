package backend.pineapple_ecommerce.scheduler;

import backend.pineapple_ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnverifiedUserCleanupScheduler {

    private final UserRepository userRepository;

    /**
     * Dọn dẹp user chưa verify email sau 48 giờ
     * Chạy lúc 2h sáng hàng ngày
     */
    @Scheduled(cron = "0 0 2 * * ?")   // 02:00 AM mỗi ngày
    @Transactional
    public void cleanupUnverifiedUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);

        int deletedCount = userRepository.deleteUnverifiedUsersOlderThan(threshold);

        if (deletedCount > 0) {
            log.info("[Cleanup] Đã xóa {} user chưa xác thực email (older than 48h)", deletedCount);
        } else {
            log.debug("[Cleanup] Không có user pending nào cần dọn dẹp");
        }
    }
}