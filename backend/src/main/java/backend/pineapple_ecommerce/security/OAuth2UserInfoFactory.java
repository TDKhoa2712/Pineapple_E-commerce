package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.enums.AuthProvider;
import backend.pineapple_ecommerce.exception.BusinessException;

import java.util.Map;

/**
 * Factory tạo OAuth2UserInfo phù hợp cho từng provider.
 *
 * Mở rộng thêm provider mới: chỉ cần thêm case mới vào switch,
 * không cần sửa bất kỳ class nào khác.
 */
public class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    /**
     * @param registrationId  provider name từ Spring Security (google, facebook, ...)
     * @param attributes      raw attributes từ OAuth2 userinfo endpoint
     * @return OAuth2UserInfo đã được abstract hóa
     */
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                                   Map<String, Object> attributes) {
        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Provider không được hỗ trợ: " + registrationId +
                    ". Các provider được hỗ trợ: GOOGLE, FACEBOOK");
        }

        return switch (provider) {
            case GOOGLE   -> new GoogleOAuth2UserInfo(attributes);
            case FACEBOOK -> new FacebookOAuth2UserInfo(attributes);
            case LOCAL    -> throw new BusinessException("LOCAL không phải OAuth2 provider");
        };
    }
}
