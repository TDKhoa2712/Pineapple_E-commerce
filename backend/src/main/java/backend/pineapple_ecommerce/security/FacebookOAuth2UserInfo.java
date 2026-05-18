package backend.pineapple_ecommerce.security;

import java.util.Map;

/**
 * Facebook OAuth2 attributes (Graph API v18+):
 * {
 *   "id":      "123456789",
 *   "name":    "Nguyen Van A",
 *   "email":   "nguyenvana@example.com",   // chỉ có khi được cấp quyền "email"
 *   "picture": {
 *     "data": {
 *       "url":          "https://platform-lookaside.fbsbx.com/...",
 *       "width":        50,
 *       "height":       50,
 *       "is_silhouette": false
 *     }
 *   }
 * }
 *
 * Lưu ý: Facebook không đảm bảo trả về email (user có thể đăng ký FB bằng SĐT).
 * CustomOAuth2UserService phải xử lý trường hợp email null.
 */
public class FacebookOAuth2UserInfo extends OAuth2UserInfo {

    public FacebookOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getImageUrl() {
        Object picture = attributes.get("picture");
        if (picture instanceof Map<?, ?> pictureMap) {
            Object pictureData = pictureMap.get("data");
            if (pictureData instanceof Map<?, ?> dataMap) {
                Object url = dataMap.get("url");
                return url instanceof String s ? s : null;
            }
        }
        return null;
    }
}
