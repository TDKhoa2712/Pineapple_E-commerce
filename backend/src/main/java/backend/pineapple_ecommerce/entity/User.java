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

    /**
     * Nullable: user đăng ký qua OAuth2 không có password.
     * Khi cần thay đổi: validation ở service layer, không ở entity.
     */
    @Column(nullable = true)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(length = 15)
    private String phone;

    /** URL ảnh đại diện (https, do Cloudinary hoặc provider cấp). */
    @Column(length = 500)
    private String avatar;

    /**
     * Public ID trên Cloudinary (chỉ có khi user tự upload ảnh).
     * Null với user OAuth2 chưa thay ảnh (ảnh lấy từ Google/Facebook).
     */
    @Column(name = "avatar_public_id", length = 300)
    private String avatarPublicId;

    // ─── OAuth2 fields ───────────────────────────────────────────────────

    /**
     * Nguồn xác thực: LOCAL (mặc định), GOOGLE, FACEBOOK.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    /**
     * ID do OAuth2 provider cấp (Google sub / Facebook id).
     * Null với user LOCAL.
     * Kết hợp với provider để tạo composite index tìm nhanh.
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

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

    // Helper
    public void addRole(Role role) {
        this.roles.add(role);
    }
}
