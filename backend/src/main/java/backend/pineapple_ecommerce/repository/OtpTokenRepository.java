package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /**
     * Tìm OTP hợp lệ (chưa dùng, chưa hết hạn) theo userId và mã OTP.
     */
    @Query("""
            SELECT o FROM OtpToken o
            WHERE o.user.id = :userId
              AND o.otp = :otp
              AND o.used = false
              AND o.expiresAt > :now
            """)
    Optional<OtpToken> findValidOtp(
            @Param("userId") Long userId,
            @Param("otp") String otp,
            @Param("now") LocalDateTime now);

    /**
     * Xoá tất cả OTP cũ của user trước khi tạo OTP mới.
     * Đảm bảo một user chỉ có một OTP active tại một thời điểm.
     */
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * Dọn dẹp OTP đã hết hạn — gọi từ scheduler định kỳ.
     */
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :threshold OR o.used = true")
    int deleteExpiredAndUsed(@Param("threshold") LocalDateTime threshold);
}
