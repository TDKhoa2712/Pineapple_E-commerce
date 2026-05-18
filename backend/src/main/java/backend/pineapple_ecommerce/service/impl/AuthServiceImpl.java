package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.LoginRequest;
import backend.pineapple_ecommerce.dto.request.RefreshTokenRequest;
import backend.pineapple_ecommerce.dto.request.RegisterRequest;
import backend.pineapple_ecommerce.dto.response.AuthResponse;
import backend.pineapple_ecommerce.entity.RefreshToken;
import backend.pineapple_ecommerce.entity.Role;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.AuthProvider;
import backend.pineapple_ecommerce.enums.RoleName;
import backend.pineapple_ecommerce.event.EmailEvents;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.mapper.UserMapper;
import backend.pineapple_ecommerce.repository.RoleRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.security.JwtService;
import backend.pineapple_ecommerce.service.AuthService;
import backend.pineapple_ecommerce.service.EmailVerificationService;
import backend.pineapple_ecommerce.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository           userRepository;
    private final RoleRepository           roleRepository;
    private final UserMapper               userMapper;
    private final PasswordEncoder          passwordEncoder;
    private final JwtService               jwtService;
    private final RefreshTokenService      refreshTokenService;
    private final UserDetailsService       userDetailsService;
    private final EmailVerificationService emailVerificationService;
    private final ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã được sử dụng: " + request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Số điện thoại đã được sử dụng: " + request.getPhone());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.ROLE_USER));
        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);

        log.info("[Auth] User registered (pending verification): {}", savedUser.getEmail());

        // Gửi OTP xác thực email
        emailVerificationService.sendVerificationOtp(savedUser.getEmail());

        // Trả về response không có JWT
        return AuthResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .emailVerified(false)
                .message("Đăng ký thành công! Vui lòng kiểm tra email để nhập mã xác thực.")
                .build();
    }

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác");
        }

        switch (user.getStatus()) {
            case INACTIVE -> throw new UnauthorizedException("Tài khoản chưa được kích hoạt");
            case BANNED   -> throw new UnauthorizedException("Tài khoản đã bị khoá");
            default       -> { /* ACTIVE */ }
        }

        if (AuthProvider.LOCAL.equals(user.getProvider())
                && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UnauthorizedException(
                    "Email chưa được xác thực. Vui lòng kiểm tra hộp thư và nhập mã OTP.");
        }

        log.info("[Auth] User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginAfterVerification(String email) {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!AuthProvider.LOCAL.equals(user.getProvider())) {
            throw new BusinessException("Chỉ áp dụng cho tài khoản LOCAL");
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException("Email chưa được xác thực");
        }
        if (user.getStatus() != backend.pineapple_ecommerce.enums.UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản không hợp lệ");
        }

        log.info("[Auth] JWT cấp sau email verification cho: {}", email);

        // Publish welcome event
        eventPublisher.publishEvent(
                new EmailEvents.UserRegisteredEvent(user.getEmail(), user.getFullName()));

        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken   = jwtService.generateAccessToken(userDetails);
        RefreshToken newRefresh = refreshTokenService.createRefreshToken(user);

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefresh.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }

    // ─────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        try {
            RefreshToken rt = refreshTokenService.verifyRefreshToken(refreshTokenValue);
            refreshTokenService.revokeByUserId(rt.getUser().getId());
            log.info("[Auth] User {} logged out", rt.getUser().getEmail());
        } catch (BusinessException e) {
            log.warn("[Auth] Logout with invalid token: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken        = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .emailVerified(user.getEmailVerified())
                .build();
    }
}