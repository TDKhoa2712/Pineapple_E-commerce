package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.UpdateProfileRequest;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    /** Lấy thông tin profile của người dùng hiện tại (lấy từ SecurityContext). */
    UserResponse getMyProfile();

    /** Lấy thông tin của bất kỳ user nào theo id — dành cho Admin. */
    UserResponse getUserById(Long userId);

    /** Cập nhật thông tin cá nhân (fullName, phone). */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * Upload hoặc thay thế avatar của user.
     * <p>
     * Flow:
     * <ol>
     *   <li>Upload file mới lên Cloudinary (folder AVATAR).</li>
     *   <li>Nếu user đã có {@code avatarPublicId} cũ → gọi
     *       {@code cloudinaryService.deleteImage()} để dọn ảnh cũ.</li>
     *   <li>Lưu {@code avatar} (URL) và {@code avatarPublicId} mới vào DB.</li>
     * </ol>
     *
     * @param userId ID của user cần cập nhật avatar
     * @param file   file ảnh gửi từ client (JPEG, PNG, WebP, GIF — tối đa 10 MB)
     * @return thông tin user sau khi cập nhật
     */
    UserResponse uploadAvatar(Long userId, MultipartFile file);

    /** Lấy danh sách tất cả user phân trang — dành cho Admin. */
    PageResponse<UserResponse> getAllUsers(int page, int size);

    /** Admin khoá / mở khoá tài khoản user. */
    void toggleUserStatus(Long userId);

    /**
     * Helper nội bộ: resolve userId đang đăng nhập từ SecurityContext.
     * Dùng trong các service khác để kiểm tra quyền sở hữu tài nguyên.
     */
    Long getCurrentUserId();
}