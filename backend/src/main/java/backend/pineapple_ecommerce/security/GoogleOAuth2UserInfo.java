package backend.pineapple_ecommerce.security;

import java.util.Map;

/**
 * Google OAuth2 attributes (OpenID Connect):
 * {
 *   "sub":            "1234567890",
 *   "name":           "Nguyen Van A",
 *   "given_name":     "Van A",
 *   "family_name":    "Nguyen",
 *   "picture":        "https://lh3.googleusercontent.com/...",
 *   "email":          "nguyenvana@gmail.com",
 *   "email_verified": true,
 *   "locale":         "vi"
 * }
 */
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
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
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
