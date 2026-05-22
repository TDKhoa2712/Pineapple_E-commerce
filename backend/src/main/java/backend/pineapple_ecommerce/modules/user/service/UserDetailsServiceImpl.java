package backend.pineapple_ecommerce.modules.user.service;

import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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

        return CustomUserDetails.of(user);
    }

    /**
     * Dùng cho OAuth2 flow — giữ lại attributes từ Google/Facebook.
     * Gọi từ CustomOAuth2UserService sau khi đã find/create User entity.
     */
    @Transactional(readOnly = true)
    public CustomUserDetails loadUserByUsernameOAuth2(User user, Map<String, Object> attributes) {
        return CustomUserDetails.of(user, attributes);
    }
}
