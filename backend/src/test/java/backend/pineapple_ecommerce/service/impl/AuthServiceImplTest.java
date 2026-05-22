package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.modules.auth.dto.request.LoginRequest;
import backend.pineapple_ecommerce.modules.auth.dto.request.RefreshTokenRequest;
import backend.pineapple_ecommerce.modules.auth.dto.request.RegisterRequest;
import backend.pineapple_ecommerce.modules.auth.dto.response.AuthResponse;
import backend.pineapple_ecommerce.modules.auth.service.AuthServiceImpl;
import backend.pineapple_ecommerce.modules.cart.models.Cart;
import backend.pineapple_ecommerce.modules.auth.models.RefreshToken;
import backend.pineapple_ecommerce.modules.auth.models.Role;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.enums.RoleName;
import backend.pineapple_ecommerce.common.enums.UserStatus;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import backend.pineapple_ecommerce.modules.user.mapper.UserMapper;
import backend.pineapple_ecommerce.modules.cart.repository.CartRepository;
import backend.pineapple_ecommerce.modules.auth.repository.RoleRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.security.JwtService;
import backend.pineapple_ecommerce.modules.auth.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository      userRepository;
    @Mock private RoleRepository      roleRepository;
    @Mock private CartRepository      cartRepository;
    @Mock private UserMapper          userMapper;
    @Mock private PasswordEncoder     passwordEncoder;
    @Mock private JwtService          jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserDetailsService  userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ── Fixtures ──────────────────────────────────────────────────────

    private User activeUser;
    private Role userRole;
    private RefreshToken refreshToken;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).name(RoleName.ROLE_USER).build();

        activeUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("$2a$encoded")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token-uuid")
                .user(activeUser)
                .expiresAt(Instant.now().plusSeconds(604800))
                .build();

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com")
                .password("$2a$encoded")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // register
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterRequest buildRequest() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("newuser@example.com");
            req.setPassword("Password123!");
            req.setFullName("Tran Thi B");
            return req;
        }

        @Test
        @DisplayName("đăng ký thành công → trả về AuthResponse có accessToken")
        void givenValidRequest_shouldReturnAuthResponse() {
            RegisterRequest req = buildRequest();

            User newUser = User.builder()
                    .id(2L)
                    .email(req.getEmail())
                    .fullName(req.getFullName())
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(userRole))
                    .build();

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userMapper.toEntity(req)).thenReturn(newUser);
            when(passwordEncoder.encode(req.getPassword())).thenReturn("$2a$new");
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(userRepository.save(newUser)).thenReturn(newUser);
            when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());
            when(userDetailsService.loadUserByUsername(newUser.getEmail())).thenReturn(userDetails);
            when(jwtService.generateAccessToken(userDetails)).thenReturn("access.token.here");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
            when(refreshTokenService.createRefreshToken(newUser)).thenReturn(refreshToken);

            AuthResponse response = authService.register(req);

            assertThat(response.getAccessToken()).isEqualTo("access.token.here");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token-uuid");
            assertThat(response.getEmail()).isEqualTo(req.getEmail());
        }

        @Test
        @DisplayName("email trùng → ném BusinessException")
        void givenDuplicateEmail_shouldThrowBusinessException() {
            RegisterRequest req = buildRequest();
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email đã được sử dụng");
        }

        @Test
        @DisplayName("số điện thoại trùng → ném BusinessException")
        void givenDuplicatePhone_shouldThrowBusinessException() {
            RegisterRequest req = buildRequest();
            req.setPhone("0901234567");
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userRepository.existsByPhone(req.getPhone())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Số điện thoại đã được sử dụng");
        }

        @Test
        @DisplayName("role ROLE_USER không tồn tại trong DB → ném ResourceNotFoundException")
        void givenMissingRole_shouldThrowResourceNotFoundException() {
            RegisterRequest req = buildRequest();
            User newUser = User.builder().email(req.getEmail()).roles(Set.of()).build();

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userMapper.toEntity(req)).thenReturn(newUser);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("đăng ký thành công → Cart mới được tạo cho user")
        void givenSuccessfulRegister_shouldCreateCart() {
            RegisterRequest req = buildRequest();
            User newUser = User.builder()
                    .id(2L).email(req.getEmail()).status(UserStatus.ACTIVE).roles(Set.of(userRole)).build();

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userMapper.toEntity(req)).thenReturn(newUser);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(userRepository.save(newUser)).thenReturn(newUser);
            when(cartRepository.save(any())).thenReturn(new Cart());
            when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
            when(jwtService.generateAccessToken(any())).thenReturn("tok");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
            when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

            authService.register(req);

            ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(cartCaptor.capture());
            assertThat(cartCaptor.getValue().getUser()).isEqualTo(newUser);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // login
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest buildRequest() {
            LoginRequest req = new LoginRequest();
            req.setEmail("user@example.com");
            req.setPassword("Password123!");
            return req;
        }

        @Test
        @DisplayName("đăng nhập thành công → trả về AuthResponse")
        void givenValidCredentials_shouldReturnAuthResponse() {
            LoginRequest req = buildRequest();
            when(userRepository.findByEmailWithRoles(req.getEmail())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(req.getPassword(), activeUser.getPassword())).thenReturn(true);
            when(userDetailsService.loadUserByUsername(activeUser.getEmail())).thenReturn(userDetails);
            when(jwtService.generateAccessToken(userDetails)).thenReturn("access.tok");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
            when(refreshTokenService.createRefreshToken(activeUser)).thenReturn(refreshToken);

            AuthResponse resp = authService.login(req);

            assertThat(resp.getAccessToken()).isEqualTo("access.tok");
            assertThat(resp.getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("email không tồn tại → ném UnauthorizedException")
        void givenUnknownEmail_shouldThrowUnauthorizedException() {
            LoginRequest req = buildRequest();
            when(userRepository.findByEmailWithRoles(req.getEmail())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("sai mật khẩu → ném UnauthorizedException")
        void givenWrongPassword_shouldThrowUnauthorizedException() {
            LoginRequest req = buildRequest();
            when(userRepository.findByEmailWithRoles(req.getEmail())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(req.getPassword(), activeUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("mật khẩu");
        }

        @Test
        @DisplayName("tài khoản INACTIVE → ném UnauthorizedException chứa 'kích hoạt'")
        void givenInactiveUser_shouldThrowWithActivationMessage() {
            LoginRequest req = buildRequest();
            activeUser.setStatus(UserStatus.INACTIVE);
            when(userRepository.findByEmailWithRoles(req.getEmail())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("kích hoạt");
        }

        @Test
        @DisplayName("tài khoản BANNED → ném UnauthorizedException chứa 'khoá'")
        void givenBannedUser_shouldThrowWithBannedMessage() {
            LoginRequest req = buildRequest();
            activeUser.setStatus(UserStatus.BANNED);
            when(userRepository.findByEmailWithRoles(req.getEmail())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("khoá");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // refreshToken
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken()")
    class Refresh { // Đã đổi tên để tránh trùng với entity.RefreshToken

        @Test
        @DisplayName("refresh token hợp lệ → trả về AuthResponse mới với token mới")
        void givenValidRefreshToken_shouldReturnNewAuthResponse() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("refresh-token-uuid");

            RefreshToken newRefresh = RefreshToken.builder()
                    .token("new-refresh-uuid")
                    .user(activeUser)
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .build();

            when(refreshTokenService.verifyRefreshToken("refresh-token-uuid")).thenReturn(refreshToken);
            when(userDetailsService.loadUserByUsername(activeUser.getEmail())).thenReturn(userDetails);
            when(jwtService.generateAccessToken(userDetails)).thenReturn("new.access.tok");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
            when(refreshTokenService.createRefreshToken(activeUser)).thenReturn(newRefresh);

            AuthResponse resp = authService.refreshToken(req);

            assertThat(resp.getAccessToken()).isEqualTo("new.access.tok");
            assertThat(resp.getRefreshToken()).isEqualTo("new-refresh-uuid");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // logout
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("logout với refresh token hợp lệ → thu hồi token thành công")
        void givenValidToken_shouldRevokeSuccessfully() {
            when(refreshTokenService.verifyRefreshToken("refresh-token-uuid")).thenReturn(refreshToken);

            authService.logout("refresh-token-uuid");

            verify(refreshTokenService).revokeByUserId(activeUser.getId());
        }

        @Test
        @DisplayName("logout với token không hợp lệ → không throw, xử lý gracefully")
        void givenInvalidToken_shouldNotThrow() {
            when(refreshTokenService.verifyRefreshToken("bad-token"))
                    .thenThrow(new BusinessException("Token không hợp lệ"));

            // Không nên throw exception — logout luôn thành công từ phía client
            authService.logout("bad-token");

            verify(refreshTokenService, never()).revokeByUserId(any());
        }
    }
}