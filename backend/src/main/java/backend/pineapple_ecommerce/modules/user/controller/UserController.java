package backend.pineapple_ecommerce.modules.user.controller;

import backend.pineapple_ecommerce.common.enums.UserStatus;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import backend.pineapple_ecommerce.modules.auth.dto.request.AdminResetPasswordRequest;
import backend.pineapple_ecommerce.modules.auth.dto.request.ChangePasswordRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateProfileRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateUserRolesRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateUserStatusRequest;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.user.dto.response.UserResponse;
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
    // AUTHENTICATED USER — tự quản lý tài khoản
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

    /**
     * POST /api/v1/users/me/change-password
     *
     * User tự đổi mật khẩu — bắt buộc xác nhận mật khẩu cũ.
     * Trả 204 No Content khi thành công.
     */
    @Operation(summary = "Đổi mật khẩu (yêu cầu mật khẩu hiện tại)")
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        Long userId = userService.getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }

    // ─────────────────────────────────────────────
    // ADMIN — xem & tìm kiếm user
    // ─────────────────────────────────────────────

    /**
     * GET /api/v1/users?page=0&size=20&status=ACTIVE&keyword=nguyen
     *
     * Lấy danh sách user với filter nâng cao:
     * - status: lọc theo trạng thái (ACTIVE / INACTIVE / BANNED), bỏ trống = tất cả
     * - keyword: tìm theo email hoặc fullName
     */
    @Operation(summary = "Danh sách tất cả người dùng, có filter (Admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false)    String keyword) {

        return ResponseEntity.ok(ApiResponse.success(
                userService.getAllUsers(page, size, status, keyword)));
    }

    @Operation(summary = "Lấy thông tin người dùng theo ID (Admin)")
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(userId)));
    }

    // ─────────────────────────────────────────────
    // ADMIN — khóa / mở khóa tài khoản
    // ─────────────────────────────────────────────

    /**
     * PATCH /api/v1/users/{userId}/status
     *
     * Cập nhật trạng thái tài khoản.
     * Body: { "status": "BANNED", "reason": "Vi phạm chính sách" }
     *
     * Các trạng thái hợp lệ: ACTIVE | INACTIVE | BANNED
     * Admin không thể tự khoá tài khoản của mình.
     */
    @Operation(summary = "Cập nhật trạng thái tài khoản (Admin)")
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        Long adminId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateUserStatus(userId, adminId, request),
                "Cập nhật trạng thái tài khoản thành công"));
    }

    // ─────────────────────────────────────────────
    // ADMIN — phân quyền role
    // ─────────────────────────────────────────────

    /**
     * PUT /api/v1/users/{userId}/roles
     *
     * Gán lại roles cho user (REPLACE toàn bộ, không append).
     * ROLE_USER luôn được giữ ngầm định dù admin không gửi lên.
     * Body: { "roles": ["ROLE_FARMER"] }
     *
     * Quy tắc an toàn:
     * - Admin không thể tự gỡ ROLE_ADMIN của chính mình.
     */
    @Operation(summary = "Gán lại roles cho user (Admin)")
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {

        Long adminId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateUserRoles(userId, adminId, request),
                "Cập nhật quyền thành công"));
    }

    // ─────────────────────────────────────────────
    // ADMIN — đặt lại mật khẩu
    // ─────────────────────────────────────────────

    /**
     * POST /api/v1/users/{userId}/reset-password
     *
     * Admin đặt lại mật khẩu cho user không cần biết mật khẩu cũ.
     * Dùng khi user yêu cầu hỗ trợ qua kênh khác (điện thoại, email).
     * Body: { "newPassword": "NewSecure@123" }
     */
    @Operation(summary = "Đặt lại mật khẩu cho user (Admin)")
    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminResetPasswordRequest request) {

        userService.adminResetPassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đặt lại mật khẩu thành công"));
    }
}
