package backend.pineapple_ecommerce.entity;

import backend.pineapple_ecommerce.enums.OtpType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lưu OTP dùng cho cả Password Reset và Email Verification.
 *
 * <p>Thay đổi so với phiên bản cũ:
 * <ul>
 *   <li>Thêm field {@code type} (OtpType enum) — phân biệt mục đích OTP</li>
 *   <li>Index {@code idx_otp_user_type} trên (user_id, type) — query nhanh hơn</li>
 *   <li>Backward-compatible: code cũ dùng PASSWORD_RESET vẫn hoạt động bình thường</li>
 * </ul>
 *
 * <p>Thiết kế bảo mật:
 * <ul>
 *   <li>Một user + một type tại một thời điểm chỉ có một OTP active</li>
 *   <li>OTP hết hạn sau 10 phút</li>
 *   <li>Sau khi dùng, đánh dấu used = true (audit trail)</li>
 *   <li>Scheduler dọn dẹp định kỳ qua {@code deleteExpiredAndUsed()}</li>
 * </ul>
 */
@Entity
@Table(
        name = "otp_tokens",
        indexes = {
                @Index(name = "idx_otp_user_type", columnList = "user_id, type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Mã OTP 6 chữ số */
    @Column(nullable = false, length = 6)
    private String otp;

    /** Thời điểm hết hạn — mặc định 10 phút từ lúc tạo */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Đã được sử dụng chưa — tránh replay attack */
    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    /**
     * Mục đích của OTP: PASSWORD_RESET hoặc EMAIL_VERIFICATION.
     * Default PASSWORD_RESET để backward-compatible với code cũ.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OtpType type = OtpType.PASSWORD_RESET;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !this.used && !isExpired();
    }
}