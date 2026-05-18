package backend.pineapple_ecommerce.enums;

/**
 * Nguồn xác thực của user.
 * LOCAL  — đăng ký trực tiếp bằng email + password.
 * GOOGLE — đăng nhập qua Google OAuth2.
 * FACEBOOK — đăng nhập qua Facebook OAuth2.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    FACEBOOK
}
