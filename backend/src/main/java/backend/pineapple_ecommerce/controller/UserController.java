package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.UpdateProfileRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.UserResponse;
import backend.pineapple_ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Users", description = "Quản lý thông tin người dùng")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ─────────────────────────────────────────────
    // AUTHENTICATED USER
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy thông tin profile của tôi")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile()));
    }

    @Operation(summary = "Cập nhật thông tin cá nhân")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateProfile(userId, request), "Cập nhật thông tin thành công"));
    }
    @Operation(summary = "Upload avatar của tôi")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                userService.uploadAvatar(userId, file), "Cập nhật avatar thành công"));
    }


    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách tất cả người dùng (Admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size)));
    }

    @Operation(summary = "Lấy thông tin người dùng theo ID (Admin)")
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(userId)));
    }

    @Operation(summary = "Khoá / Mở khoá tài khoản người dùng (Admin)")
    @PatchMapping("/{userId}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long userId) {
        userService.toggleUserStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật trạng thái tài khoản"));
    }
}
