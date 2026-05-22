package backend.pineapple_ecommerce.modules.auth.service;

import backend.pineapple_ecommerce.modules.auth.dto.response.AuthResponse;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.security.OAuth2UserInfo;

/**
 * OAuth2Service — tách biệt business logic OAuth2 khỏi Spring Security infrastructure.
 *
 * Lý do tách:
 * - CustomOAuth2UserService là Spring Security component (extends DefaultOAuth2UserService)
 *   → Nên giữ mỏng, chỉ làm cầu nối giữa Spring Security và business layer
 * - Logic tìm/tạo user, tạo cart, link account → thuộc về service layer
 * - Dễ test hơn (mock OAuth2Service thay vì mock toàn bộ OAuth2 stack)
 * - Có thể reuse trong tương lai (link account từ profile page, v.v.)
 */
public interface OAuth2Service {

    /**
     * Xử lý OAuth2 user: tìm theo email/providerId, tạo mới hoặc update.
     *
     * @param registrationId  tên provider (google, facebook)
     * @param userInfo        thông tin đã được abstract hóa từ provider attributes
     * @return User entity đã được persist (mới hoặc đã tồn tại)
     */
    User processOAuth2User(String registrationId, OAuth2UserInfo userInfo);

    /**
     * Tạo AuthResponse (JWT access + refresh token) cho user OAuth2.
     * Dùng chung infrastructure với LOCAL login flow.
     *
     * @param user User entity đã xác thực
     * @return AuthResponse với đầy đủ token, userId, roles
     */
    AuthResponse buildAuthResponse(User user);
}
