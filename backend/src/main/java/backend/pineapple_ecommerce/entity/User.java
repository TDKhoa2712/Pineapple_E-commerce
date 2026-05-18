package backend.pineapple_ecommerce.entity;

import backend.pineapple_ecommerce.enums.AuthProvider;
import backend.pineapple_ecommerce.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

/**
 * User entity — bổ sung OAuth2 fields:
 *   - provider     : nguồn xác thực (LOCAL / GOOGLE / FACEBOOK)
 *   - providerId   : ID do OAuth2 provider cấp (sub của Google, id của Facebook)
 *   - password     : nullable — user OAuth2 không có password
 *
 * Index bổ sung:
 *   - idx_users_email          (đã unique → tự có index)
 *   - idx_users_provider_id    (composite provider + provider_id) — tìm user OAuth2 nhanh
 *
 * Lưu ý: KHÔNG thêm @Column(unique) cho providerId vì mỗi provider có không gian ID
 * riêng; composite index (provider, providerId) đủ để đảm bảo tính duy nhất.
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email",       columnList = "email"),
                @Index(name = "idx_users_provider_id", columnList = "provider, provider_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** Nullable — OAuth2 user không có password. */
    @Column(nullable = true)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(length = 15)
    private String phone;

    @Column(length = 500)
    private String avatar;

    @Column(name = "avatar_public_id", length = 300)
    private String avatarPublicId;

    // ─── OAuth2 fields ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    /**
     * Email đã được xác minh chưa.
     * - OAuth2 user: true ngay từ đầu (Google/Facebook đã xác minh)
     * - LOCAL user: false mặc định, có thể set true qua email verification flow
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    // ─── Status & Roles ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Cart cart;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Wishlist> wishlistItems = new ArrayList<>();

    public void addRole(Role role) {
        this.roles.add(role);
    }
}
