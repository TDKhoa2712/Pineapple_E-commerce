package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.OtpToken;
import backend.pineapple_ecommerce.enums.OtpType;
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
     * Tìm OTP hợp lệ (chưa dùng, chưa hết hạn) theo userId, mã OTP VÀ type.
     * Tách biệt query theo type để tránh dùng lẫn OTP giữa các flow.
     */
    @Query("""
            SELECT o FROM OtpToken o
            WHERE o.user.id = :userId
              AND o.otp = :otp
              AND o.type = :type
              AND o.used = false
              AND o.expiresAt > :now
            """)
    Optional<OtpToken> findValidOtp(
            @Param("userId") Long userId,
            @Param("otp") String otp,
            @Param("type") OtpType type,
            @Param("now") LocalDateTime now);

    /**
     * [Backward-compat] Query cũ không có type — dùng cho PASSWORD_RESET nếu cần.
     * Khuyến nghị: migrate sang findValidOtp(userId, otp, type, now).
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
     * Xoá tất cả OTP của user theo type — gọi trước khi tạo OTP mới.
     * Đảm bảo một user chỉ có một OTP active mỗi type tại một thời điểm.
     */
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.user.id = :userId AND o.type = :type")
    void deleteAllByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") OtpType type);

    /**
     * Xoá tất cả OTP của user (mọi type) — dùng khi xoá user.
     */
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * Kiểm tra có OTP chưa dùng của user+type được tạo sau mốc thời gian không.
     * Dùng để rate-limit: không cho phép gửi lại quá thường xuyên.
     */
    @Query("""
            SELECT COUNT(o) > 0 FROM OtpToken o
            WHERE o.user.id = :userId
              AND o.type = :type
              AND o.used = false
              AND o.createdAt > :after
            """)
    boolean existsRecentOtp(
            @Param("userId") Long userId,
            @Param("type") OtpType type,
            @Param("after") LocalDateTime after);

    /**
     * Đếm số OTP đã gửi của user+type trong khoảng thời gian (dùng cho rate limit).
     */
    @Query("""
            SELECT COUNT(o) FROM OtpToken o
            WHERE o.user.id = :userId
              AND o.type = :type
              AND o.createdAt > :after
            """)
    long countOtpSentSince(
            @Param("userId") Long userId,
            @Param("type") OtpType type,
            @Param("after") LocalDateTime after);

    /**
     * Dọn dẹp OTP đã hết hạn hoặc đã dùng — gọi từ scheduler định kỳ.
     */
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :threshold OR o.used = true")
    int deleteExpiredAndUsed(@Param("threshold") LocalDateTime threshold);
}