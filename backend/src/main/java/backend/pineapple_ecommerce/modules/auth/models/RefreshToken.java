package backend.pineapple_ecommerce.modules.auth.models;

import backend.pineapple_ecommerce.modules.user.models.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
        @Index(name = "idx_refresh_tokens_last_used", columnList = "last_used_at") // Thêm luôn index cho cột last_used_at
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}
