package backend.pineapple_ecommerce.security;

import java.util.Map;

/**
 * Abstract class chuẩn hóa thông tin user từ các OAuth2 provider khác nhau.
 *
 * Google trả về: sub, email, name, picture
 * Facebook trả về: id, email, name, picture.data.url
 *
 * Bằng cách abstract hóa, CustomOAuth2UserService không cần biết provider cụ thể
 * mà vẫn lấy được đầy đủ thông tin.
 */
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** ID duy nhất do provider cấp (Google: "sub", Facebook: "id"). */
    public abstract String getId();

    /** Tên hiển thị của user. */
    public abstract String getName();

    /** Email của user (Google luôn có; Facebook cần quyền email). */
    public abstract String getEmail();

    /** URL ảnh đại diện từ provider (nullable). */
    public abstract String getImageUrl();
}
