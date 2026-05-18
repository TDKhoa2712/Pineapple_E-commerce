package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.entity.Cart;
import backend.pineapple_ecommerce.entity.Role;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.AuthProvider;
import backend.pineapple_ecommerce.enums.RoleName;
import backend.pineapple_ecommerce.enums.UserStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.repository.CartRepository;
import backend.pineapple_ecommerce.repository.RoleRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Xử lý core logic OAuth2:
 * 1. Gọi provider userinfo endpoint (qua DefaultOAuth2UserService delegate)
 * 2. Tìm user theo email trong DB
 * 3. Nếu chưa có → tạo mới với role CUSTOMER, set provider + providerId
 * 4. Nếu đã có → update fullName + avatar từ provider (không override avatar đã custom)
 * 5. Trả về CustomUserDetails để Spring Security dùng trong authentication context
 *
 * Hiệu năng:
 * - Sử dụng findByEmailWithRoles() thay vì findByEmail() để tránh N+1 query với roles
 * - Index trên (provider, provider_id) đã được khai báo ở User entity
 * - @Transactional đảm bảo toàn bộ find + update/create là atomic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Bước 1: Gọi userinfo endpoint của provider (Google/Facebook)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (BusinessException ex) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oauth2_processing_error"), ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("OAuth2 processing error", ex);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oauth2_processing_error"),
                    "Lỗi xử lý đăng nhập OAuth2: " + ex.getMessage(), ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // Bước 2: Parse attributes theo từng provider
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oAuth2User.getAttributes());

        // Validate email — Facebook có thể không trả email
        if (!StringUtils.hasText(userInfo.getEmail())) {
            throw new BusinessException(
                    "Không lấy được email từ " + registrationId +
                    ". Vui lòng cấp quyền truy cập email cho ứng dụng.");
        }

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // Bước 3: Tìm hoặc tạo user
        User user = userRepository.findByEmailWithRoles(userInfo.getEmail())
                .map(existingUser -> updateExistingUser(existingUser, userInfo, provider))
                .orElseGet(() -> registerNewUser(userInfo, provider));

        // Bước 4: Wrap vào CustomUserDetails (dùng chung với JWT flow)
        return UserDetailsServiceImpl.buildUserDetails(user, oAuth2User.getAttributes());
    }

    /**
     * Cập nhật thông tin user đã tồn tại.
     * Chỉ update fullName + avatar nếu user CHƯA có avatar custom (avatarPublicId null).
     * Không bao giờ override provider sang LOCAL nếu user đã đăng nhập OAuth2.
     */
    private User updateExistingUser(User user, OAuth2UserInfo userInfo, AuthProvider provider) {
        // Kiểm tra conflict: user đăng ký LOCAL nhưng email trùng với OAuth2
        if (user.getProvider() == AuthProvider.LOCAL) {
            log.warn("Email {} đã đăng ký LOCAL, đang liên kết với provider {}",
                    user.getEmail(), provider);
            // Có thể throw BusinessException yêu cầu user đăng nhập LOCAL rồi link account
            // Hiện tại: cho phép đăng nhập và cập nhật provider
        }

        user.setFullName(userInfo.getName());

        // Không override avatar nếu user đã tự upload (có avatarPublicId trên Cloudinary)
        if (!StringUtils.hasText(user.getAvatarPublicId()) && StringUtils.hasText(userInfo.getImageUrl())) {
            user.setAvatar(userInfo.getImageUrl());
        }

        // Cập nhật provider info nếu chưa có
        if (user.getProvider() != provider) {
            user.setProvider(provider);
            user.setProviderId(userInfo.getId());
        }

        User saved = userRepository.save(user);
        log.info("OAuth2 user updated: {} via {}", saved.getEmail(), provider);
        return saved;
    }

    /**
     * Tạo user mới từ OAuth2 — không có password, role mặc định ROLE_USER.
     * Tự động tạo Cart trống như register flow thông thường.
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
                .status(UserStatus.ACTIVE)  // Auto-active vì đã xác thực qua provider
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
