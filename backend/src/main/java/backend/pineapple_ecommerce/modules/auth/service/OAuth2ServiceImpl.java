package backend.pineapple_ecommerce.modules.auth.service;

import backend.pineapple_ecommerce.common.enums.AuthProvider;
import backend.pineapple_ecommerce.modules.auth.dto.response.AuthResponse;
import backend.pineapple_ecommerce.modules.cart.models.Cart;
import backend.pineapple_ecommerce.modules.auth.models.RefreshToken;
import backend.pineapple_ecommerce.modules.auth.models.Role;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.enums.RoleName;
import backend.pineapple_ecommerce.common.enums.UserStatus;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.modules.cart.repository.CartRepository;
import backend.pineapple_ecommerce.modules.auth.repository.RoleRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.security.CustomUserDetails;
import backend.pineapple_ecommerce.security.OAuth2UserInfo;
import backend.pineapple_ecommerce.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2ServiceImpl — toàn bộ business logic OAuth2.
 *
 * Trách nhiệm:
 * - Tìm/tạo/update User từ OAuth2UserInfo
 * - Tạo Cart cho user mới (giống register LOCAL)
 * - Xử lý Account Linking (email conflict giữa LOCAL và OAuth2)
 * - Tạo AuthResponse (JWT) để SuccessHandler trả về FE
 *
 * KHÔNG thuộc trách nhiệm:
 * - Gọi provider userinfo endpoint (CustomOAuth2UserService làm)
 * - Redirect về FE (OAuth2AuthenticationSuccessHandler làm)
 * - Spring Security filter chain
 *
 * Account Linking Strategy (có thể cấu hình):
 * Hiện tại: AUTO_LINK — nếu email trùng với user LOCAL thì tự động link,
 * update provider info và cho đăng nhập. Đây là trải nghiệm user tốt hơn
 * so với throw error, vì user có thể quên họ đã đăng ký LOCAL trước đó.
 * Trade-off: nếu email bị compromise, attacker có thể link OAuth2 vào account.
 * Mitigated bởi: provider đã xác thực email ownership (Google verified email).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private final UserRepository      userRepository;
    private final RoleRepository      roleRepository;
    private final CartRepository      cartRepository;
    private final JwtService          jwtService;
    private final RefreshTokenService refreshTokenService;

    // ─────────────────────────────────────────────
    // PROCESS OAUTH2 USER
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public User processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // Validate email — Facebook có thể không trả email (user đăng ký FB bằng SĐT)
        if (!StringUtils.hasText(userInfo.getEmail())) {
            throw new BusinessException(
                    "Không lấy được email từ " + registrationId +
                    ". Vui lòng cấp quyền truy cập email cho ứng dụng.");
        }

        // Chiến lược tìm kiếm: ưu tiên providerId (nhanh hơn, chính xác hơn)
        // Fallback về email nếu chưa có record với providerId (user mới hoặc user LOCAL)
        return userRepository
                .findByProviderAndProviderId(provider, userInfo.getId())
                .map(user -> updateExistingUser(user, userInfo))
                .orElseGet(() -> userRepository
                        .findByEmailWithRoles(userInfo.getEmail())
                        .map(user -> linkOrUpdateUser(user, userInfo, provider))
                        .orElseGet(() -> registerNewUser(userInfo, provider)));
    }

    // ─────────────────────────────────────────────
    // BUILD AUTH RESPONSE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse buildAuthResponse(User user) {
        // Dùng CustomUserDetails — nhất quán với JWT flow
        CustomUserDetails userDetails = CustomUserDetails.of(user);

        String accessToken  = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    /**
     * User đã đăng nhập OAuth2 trước đó (tìm được bằng providerId).
     * Chỉ update display info (name, avatar), không đổi provider.
     */
    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        if (StringUtils.hasText(userInfo.getName())) {
            user.setFullName(userInfo.getName());
        }
        // Chỉ update avatar nếu user chưa tự upload lên Cloudinary
        if (!StringUtils.hasText(user.getAvatarPublicId())
                && StringUtils.hasText(userInfo.getImageUrl())) {
            user.setAvatar(userInfo.getImageUrl());
        }
        User saved = userRepository.save(user);
        log.debug("OAuth2 user info updated: {}", saved.getEmail());
        return saved;
    }

    /**
     * Email đã tồn tại nhưng chưa có providerId match.
     * Hai trường hợp:
     *   A) User LOCAL → AUTO_LINK: cập nhật provider + providerId
     *   B) User OAuth2 khác provider (Google ↔ Facebook) → cũng link
     *
     * Security note: Google và Facebook đều đã xác minh quyền sở hữu email.
     * Auto-link ở đây an toàn vì nếu email A đã verified bởi Google,
     * thì người dùng Facebook có email A cũng chính là chủ của email đó.
     */
    private User linkOrUpdateUser(User user, OAuth2UserInfo userInfo, AuthProvider provider) {
        if (user.getProvider() != provider) {
            log.info("Account linking: email={} từ {} → link thêm {}",
                    user.getEmail(), user.getProvider(), provider);
            user.setProvider(provider);
            user.setProviderId(userInfo.getId());
        }

        return updateExistingUser(user, userInfo);
    }

    /**
     * User hoàn toàn mới — tạo account + Cart, không cần password.
     * emailVerified = true vì provider đã xác minh.
     */
    private User registerNewUser(OAuth2UserInfo userInfo, AuthProvider provider) {
        Role customerRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new BusinessException("Role ROLE_USER không tồn tại trong DB"));

        User newUser = User.builder()
                .email(userInfo.getEmail())
                .fullName(userInfo.getName())
                .avatar(userInfo.getImageUrl())
                .provider(provider)
                .providerId(userInfo.getId())
                .password(null)             // OAuth2 user không có password
                .emailVerified(true)        // Email đã được provider xác minh
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build();

        User savedUser = userRepository.save(newUser);

        // Tạo Cart trống — giống register flow LOCAL
        Cart cart = Cart.builder().user(savedUser).build();
        cartRepository.save(cart);

        log.info("New OAuth2 user registered: {} via {}", savedUser.getEmail(), provider);
        return savedUser;
    }
}
