package backend.pineapple_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lưu OTP dùng để reset mật khẩu.
 *
 * <p>Thiết kế:
 * <ul>
 *   <li>Một user tại một thời điểm chỉ có một OTP active (xoá cũ trước khi tạo mới)</li>
 *   <li>OTP hết hạn sau 10 phút (expiresAt)</li>
 *   <li>Sau khi dùng, đánh dấu used = true thay vì xoá ngay (audit trail)</li>
 *   <li>Scheduler dọn dẹp OTP đã hết hạn/đã dùng định kỳ nếu cần</li>
 * </ul>
 *
 * <p>DDL tương ứng (auto-generated bởi JPA):
 * <pre>
 * CREATE TABLE otp_tokens (
 *   id         BIGINT AUTO_INCREMENT PRIMARY KEY,
 *   user_id    BIGINT NOT NULL,
 *   otp        VARCHAR(6) NOT NULL,
 *   expires_at DATETIME NOT NULL,
 *   used       BOOLEAN NOT NULL DEFAULT FALSE,
 *   created_at DATETIME NOT NULL,
 *   CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id)
 * );
 * </pre>
 */
@Entity
@Table(name = "otp_tokens")
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
