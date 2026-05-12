package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test cho JwtService.
 * Không cần Spring context — khởi tạo thủ công với JwtProperties stub.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    // Secret đủ 256-bit để JJWT không từ chối
    private static final String SECRET =
            "my-super-secret-key-for-testing-purposes-1234567890abcdef";
    private static final long ACCESS_EXPIRY_MS  = 900_000L;  // 15 phút
    private static final long REFRESH_EXPIRY_MS = 604_800_000L;

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTokenExpirationMs(ACCESS_EXPIRY_MS);
        props.setRefreshTokenExpirationMs(REFRESH_EXPIRY_MS);

        jwtService = new JwtService(props);

        userDetails = User.withUsername("user@example.com")
                .password("hashed")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // generateAccessToken
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessToken {

        @Test
        @DisplayName("trả về chuỗi JWT không rỗng")
        void shouldReturnNonBlankToken() {
            String token = jwtService.generateAccessToken(userDetails);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("token có 3 phần phân cách bởi dấu chấm (header.payload.signature)")
        void shouldHaveThreeParts() {
            String token = jwtService.generateAccessToken(userDetails);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("username trong token khớp với UserDetails")
        void shouldEmbedCorrectUsername() {
            String token = jwtService.generateAccessToken(userDetails);
            assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("token chưa hết hạn ngay sau khi tạo")
        void shouldNotBeExpiredRightAfterCreation() {
            String token = jwtService.generateAccessToken(userDetails);
            assertThat(jwtService.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("extraClaims được nhúng vào token")
        void givenExtraClaims_shouldBeExtractable() {
            String token = jwtService.generateAccessToken(
                    java.util.Map.of("role", "ADMIN"), userDetails);
            io.jsonwebtoken.Claims claims = jwtService.extractClaim(token, c -> c);
            assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // isTokenValid
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("token hợp lệ đúng user → true")
        void givenValidTokenAndMatchingUser_shouldReturnTrue() {
            String token = jwtService.generateAccessToken(userDetails);
            assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("token hợp lệ nhưng sai user → false")
        void givenValidTokenButDifferentUser_shouldReturnFalse() {
            String token = jwtService.generateAccessToken(userDetails);

            UserDetails otherUser = User.withUsername("other@example.com")
                    .password("x")
                    .authorities(List.of())
                    .build();

            assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
        }

        @Test
        @DisplayName("token giả mạo → false (không throw)")
        void givenTamperedToken_shouldReturnFalse() {
            String fakeToken = "this.is.not.a.valid.jwt";
            assertThat(jwtService.isTokenValid(fakeToken, userDetails)).isFalse();
        }

        @Test
        @DisplayName("token hết hạn → false")
        void givenExpiredToken_shouldReturnFalse() {
            // Tạo service với expiry = 1ms để token hết hạn ngay lập tức
            JwtProperties shortLivedProps = new JwtProperties();
            shortLivedProps.setSecret(SECRET);
            shortLivedProps.setAccessTokenExpirationMs(1L);

            JwtService shortLivedService = new JwtService(shortLivedProps);
            String token = shortLivedService.generateAccessToken(userDetails);

            // Đợi token hết hạn
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            assertThat(shortLivedService.isTokenValid(token, userDetails)).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // getAccessTokenExpirationMs
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAccessTokenExpirationMs()")
    class GetExpiration {

        @Test
        @DisplayName("trả về giá trị từ JwtProperties")
        void shouldReturnConfiguredValue() {
            assertThat(jwtService.getAccessTokenExpirationMs()).isEqualTo(ACCESS_EXPIRY_MS);
        }
    }
}