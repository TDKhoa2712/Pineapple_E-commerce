package backend.pineapple_ecommerce.modules.auth.service;

import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.enums.UserStatus;
import backend.pineapple_ecommerce.security.CustomUserDetails;
import backend.pineapple_ecommerce.security.OAuth2UserInfo;
import backend.pineapple_ecommerce.security.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
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

    private final OAuth2Service oauth2Service;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Bước 1: Gọi provider userinfo endpoint — Spring Security lo phần này
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (OAuth2AuthenticationException ex) {
            throw ex;   // Re-throw, đừng wrap thêm
        } catch (Exception ex) {
            log.error("OAuth2 processing error for provider {}: {}",
                    userRequest.getClientRegistration().getRegistrationId(), ex.getMessage(), ex);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oauth2_processing_error"),
                    "Lỗi xử lý đăng nhập OAuth2: " + ex.getMessage(), ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // Bước 2: Parse attributes theo từng provider (factory pattern)
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oAuth2User.getAttributes());

        // Bước 3: Delegate toàn bộ business logic sang OAuth2Service
        User user = oauth2Service.processOAuth2User(registrationId, userInfo);

        // Kiểm tra trạng thái người dùng sau khi tạo / lấy từ DB
        if (user.getStatus() == UserStatus.BANNED) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_banned"), "Tài khoản đã bị khoá");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_inactive"), "Tài khoản chưa được kích hoạt");
        }

        // Bước 4: Wrap thành CustomUserDetails — implements cả UserDetails + OAuth2User
        // Giữ nguyên attributes từ provider để SuccessHandler có thể đọc email
        return CustomUserDetails.of(user, oAuth2User.getAttributes());
    }
}
