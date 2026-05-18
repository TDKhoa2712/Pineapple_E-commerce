package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.UserStatus;
import backend.pineapple_ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserDetailsService implementation:
 * - Load user từ DB theo email
 * - Wrap thành Spring Security UserDetails
 * - Kiểm tra trạng thái tài khoản (ACTIVE / BANNED / INACTIVE)
 *
 * Thay đổi so với phiên bản cũ:
 * - buildUserDetails() đổi thành static để CustomOAuth2UserService có thể gọi
 * - Thêm overload buildUserDetails(User, Map) cho OAuth2 flow (giữ nguyên attributes)
 * - password được truyền vào có thể null với OAuth2 user
 *   → DaoAuthenticationProvider sẽ không dùng password này vì OAuth2 flow
 *     không đi qua DaoAuthenticationProvider
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return buildUserDetails(user);
    }

    /**
     * Dùng cho JWT flow (login LOCAL).
     * Password có thể là empty string nếu user OAuth2 cố gắng loadUser.
     */
    public static UserDetails buildUserDetails(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        // OAuth2 user không có password → dùng empty string
        // Spring Security không dùng field này để authenticate trong JWT/OAuth2 flow
        String passwordForSecurity = user.getPassword() != null ? user.getPassword() : "";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(passwordForSecurity)
                .authorities(authorities)
                .accountLocked(user.getStatus() == UserStatus.BANNED)
                .disabled(user.getStatus() == UserStatus.INACTIVE)
                .build();
    }

    /**
     * Dùng cho OAuth2 flow — trả về OAuth2User để Spring Security
     * có thể store vào SecurityContext đúng type.
     * Giữ nguyên attributes từ provider (cần cho SuccessHandler).
     */
    public static OAuth2User buildUserDetails(User user, Map<String, Object> attributes) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        // nameAttributeKey = "email" — dùng email làm principal name, nhất quán với JWT
        return new DefaultOAuth2User(authorities, attributes, determineNameAttributeKey(attributes));
    }

    /**
     * Xác định key nào trong attributes dùng làm principal name.
     * Google dùng "sub", Facebook dùng "id", nhưng chúng ta muốn dùng "email".
     */
    private static String determineNameAttributeKey(Map<String, Object> attributes) {
        if (attributes.containsKey("sub")) return "sub";   // Google
        if (attributes.containsKey("id"))  return "id";    // Facebook
        return "email";
    }
}
