package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single principal class dùng chung cho cả JWT flow và OAuth2 flow.
 *
 * Vấn đề gốc được giải quyết:
 * - JWT flow   → Spring Security mong đợi principal kiểu UserDetails
 * - OAuth2 flow → Spring Security mong đợi principal kiểu OAuth2User
 * - Nếu hai flow trả về type khác nhau → SecurityContextHolder.getContext()
 *   .getAuthentication().getPrincipal() sẽ ném ClassCastException ở bất cứ
 *   chỗ nào code giả định kiểu cụ thể.
 *
 * Giải pháp: implement CẢ HAI interface trong một class duy nhất.
 * - Khi không có OAuth2 attributes (JWT flow): attributes = emptyMap(), không gây lỗi
 * - Khi có attributes (OAuth2 flow): đầy đủ thông tin của provider
 *
 * Nhờ đó, mọi chỗ trong codebase chỉ cần cast sang CustomUserDetails một lần
 * để lấy userId, email, hay bất kỳ thông tin nào — bất kể flow nào tạo ra nó.
 *
 * Cách lấy principal trong controller:
 * {@code
 *   @AuthenticationPrincipal CustomUserDetails principal
 *   // hoặc
 *   CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
 *       .getContext().getAuthentication().getPrincipal();
 * }
 */
public class CustomUserDetails implements UserDetails, OAuth2User {

    // ── Core user data ────────────────────────────────────────────────────

    @Getter
    private final Long   userId;

    @Getter
    private final String email;

    private final String password;

    @Getter
    private final String fullName;

    @Getter
    private final String avatar;

    // ── Security fields ──────────────────────────────────────────────────

    private final Collection<GrantedAuthority> authorities;
    private final boolean accountLocked;
    private final boolean disabled;

    // ── OAuth2-only fields (null / emptyMap khi dùng JWT flow) ───────────

    /** Attributes từ Google/Facebook. Empty map với JWT flow. */
    private final Map<String, Object> oauth2Attributes;

    // ── Constructors ─────────────────────────────────────────────────────

    /**
     * Constructor cho JWT flow (LOCAL login).
     * oauth2Attributes sẽ là emptyMap — không cần attributes từ provider.
     */
    public CustomUserDetails(User user) {
        this(user, Collections.emptyMap());
    }

    /**
     * Constructor cho OAuth2 flow.
     * Giữ nguyên attributes từ provider để SuccessHandler có thể đọc email.
     */
    public CustomUserDetails(User user, Map<String, Object> oauth2Attributes) {
        this.userId    = user.getId();
        this.email     = user.getEmail();
        this.password  = user.getPassword() != null ? user.getPassword() : "";
        this.fullName  = user.getFullName();
        this.avatar    = user.getAvatar();

        this.authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toUnmodifiableList());

        this.accountLocked = user.getStatus() == UserStatus.BANNED;
        this.disabled      = user.getStatus() == UserStatus.INACTIVE;

        this.oauth2Attributes = oauth2Attributes != null
                ? Collections.unmodifiableMap(oauth2Attributes)
                : Collections.emptyMap();
    }

    // ── UserDetails impl ─────────────────────────────────────────────────

    @Override
    public String getUsername() {
        return email;   // Spring Security dùng username = email trong hệ thống này
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isEnabled() {
        return !disabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;    // Không dùng expiry theo thời gian cho account
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;    // Token expiry được xử lý bởi JWT, không phải credentials
    }

    // ── OAuth2User impl ──────────────────────────────────────────────────

    /**
     * getName() dùng email làm principal name — nhất quán giữa hai flow.
     * Spring Security dùng getName() để identify principal trong SecurityContext.
     */
    @Override
    public String getName() {
        return email;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2Attributes;
    }

    // ── Factory methods ──────────────────────────────────────────────────

    /** Dùng cho JWT flow / LOCAL login. */
    public static CustomUserDetails of(User user) {
        return new CustomUserDetails(user);
    }

    /** Dùng cho OAuth2 flow — giữ lại attributes từ Google/Facebook. */
    public static CustomUserDetails of(User user, Map<String, Object> attributes) {
        return new CustomUserDetails(user, attributes);
    }
}
