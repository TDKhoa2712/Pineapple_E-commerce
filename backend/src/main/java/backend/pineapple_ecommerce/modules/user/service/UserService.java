package backend.pineapple_ecommerce.modules.user.service;

import backend.pineapple_ecommerce.common.enums.UserStatus;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.modules.auth.dto.request.AdminResetPasswordRequest;
import backend.pineapple_ecommerce.modules.auth.dto.request.ChangePasswordRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateProfileRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateUserRolesRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateUserStatusRequest;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.user.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    // ─────────────────────────────────────────────
    // USER — thao tác trên tài khoản của chính mình
    // ─────────────────────────────────────────────

    /** Lấy thông tin profile của người dùng hiện tại (lấy từ SecurityContext). */
    UserResponse getMyProfile();

    /** Cập nhật thông tin cá nhân (fullName, phone). */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /** Upload/thay avatar. */
    UserResponse uploadAvatar(Long userId, MultipartFile file);

    /**
     * Đổi mật khẩu — user tự thực hiện, bắt buộc xác nhận mật khẩu cũ.
     * Throw BusinessException nếu:
     *   - currentPassword sai
     *   - newPassword == confirmNewPassword không khớp
     *   - newPassword trùng currentPassword
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    // ─────────────────────────────────────────────
    // ADMIN — quản lý user
    // ─────────────────────────────────────────────

    /** Lấy thông tin của bất kỳ user nào theo id. */
    UserResponse getUserById(Long userId);

    /**
     * Lấy danh sách user phân trang, có thể filter theo status.
     * status = null → trả toàn bộ.
     */
    PageResponse<UserResponse> getAllUsers(int page, int size, UserStatus status, String keyword, backend.pineapple_ecommerce.common.enums.RoleName role, String sortBy, String sortDirection);

    /**
     * Cập nhật trạng thái tài khoản (ACTIVE / INACTIVE / BANNED).
     * Admin không thể tự khoá chính mình.
     */
    UserResponse updateUserStatus(Long targetUserId, Long adminId, UpdateUserStatusRequest request);

    /**
     * Gán lại toàn bộ roles cho user (REPLACE, không append).
     * ROLE_USER luôn được giữ lại ngầm định.
     * Admin không thể tự gỡ role ADMIN của chính mình.
     */
    UserResponse updateUserRoles(Long targetUserId, Long adminId, UpdateUserRolesRequest request);

    /**
     * Admin đặt lại mật khẩu cho user mà không cần biết mật khẩu cũ.
     * Dùng khi user quên mật khẩu và chưa có flow reset-password hoàn chỉnh.
     */
    void adminResetPassword(Long targetUserId, AdminResetPasswordRequest request);

    /**
     * Admin cập nhật thông tin cá nhân của user (fullName, phone).
     * Admin không thể cập nhật email, status, roles qua endpoint này.
     */
    UserResponse updateUserAdmin(Long targetUserId, backend.pineapple_ecommerce.modules.user.dto.request.AdminUpdateUserRequest request);

    /**
     * Admin upload/thay đổi avatar cho user.
     * Xóa avatar cũ nếu tồn tại.
     */
    UserResponse uploadUserAvatarAdmin(Long targetUserId, MultipartFile file);

    /**
     * Helper nội bộ: resolve userId đang đăng nhập từ SecurityContext.
     */
    Long getCurrentUserId();

    UserResponse getUserByEmail(String email);

    User getEntityUser(Long userId);
}
