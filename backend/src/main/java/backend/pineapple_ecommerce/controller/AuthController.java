package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.LoginRequest;
import backend.pineapple_ecommerce.dto.request.RefreshTokenRequest;
import backend.pineapple_ecommerce.dto.request.RegisterRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.AuthResponse;
import backend.pineapple_ecommerce.dto.response.UserResponse;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.mapper.UserMapper;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 *
 * THAY ĐỔI so với phiên bản cũ:
 *  - logout: truyền refreshToken (từ request body) thay vì userId
 *  - me: dùng UserMapper để trả UserResponse chuẩn thay vì Map thô
 *  - refreshToken: gọi đúng tên method authService.refreshToken()
 *    (interface cũ dùng tên "refresh" nhưng interface mới dùng "refreshToken")
 */
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, refresh token, logout")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper     userMapper;

    // ─────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────

    @Operation(summary = "Đăng ký tài khoản mới")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đăng ký thành công"));
    }

    // ─────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────

    @Operation(summary = "Đăng nhập")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    // ─────────────────────────────────────────────
    // Refresh Token
    // ─────────────────────────────────────────────

    @Operation(summary = "Làm mới access token bằng refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        // FIX: gọi refreshToken() thay vì refresh()
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token đã được làm mới"));
    }

    // ─────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────

    @Operation(summary = "Đăng xuất", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        // FIX: truyền refreshToken string thay vì userId
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    // ─────────────────────────────────────────────
    // Me — thông tin user hiện tại
    // ─────────────────────────────────────────────

    @Operation(summary = "Thông tin tài khoản hiện tại",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal UserDetails userDetails) {

        // FIX: trả UserResponse qua UserMapper thay vì Map thô
        User user = userRepository.findByEmailWithRoles(userDetails.getUsername())
                .orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponse(user)));
    }
}
