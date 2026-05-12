package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.UpdateProfileRequest;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.UploadResponse;
import backend.pineapple_ecommerce.dto.response.UserResponse;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.UploadFolder;
import backend.pineapple_ecommerce.enums.UserStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.mapper.UserMapper;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.CloudinaryService;
import backend.pineapple_ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository    userRepository;
    private final UserMapper        userMapper;
    private final CloudinaryService cloudinaryService;

    // ─────────────────────────────────────────────
    // GET PROFILE
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        Long userId = getCurrentUserId();
        return userMapper.toResponse(findUserById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return userMapper.toResponse(findUserById(userId));
    }

    // ─────────────────────────────────────────────
    // UPDATE PROFILE (text fields only)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            boolean phoneTaken = userRepository.existsByPhone(request.getPhone())
                    && !request.getPhone().equals(user.getPhone());
            if (phoneTaken) {
                throw new BusinessException("Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }

        User saved = userRepository.save(user);
        log.info("User {} updated profile", userId);
        return userMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // UPLOAD / REPLACE AVATAR
    // ─────────────────────────────────────────────

    /**
     * Upload avatar mới cho user.
     *
     * <p>Flow:
     * <ol>
     *   <li>Upload file mới lên Cloudinary (folder AVATAR).</li>
     *   <li>Xoá ảnh cũ khỏi Cloudinary nếu {@code avatarPublicId} tồn tại.</li>
     *   <li>Lưu URL và publicId mới vào DB.</li>
     * </ol>
     *
     * Lưu ý: bước upload xảy ra TRƯỚC khi xoá ảnh cũ, đảm bảo
     * không mất ảnh nếu upload thất bại (exception từ uploadImage sẽ
     * propagate ra trước khi ta xoá ảnh cũ).
     */
    @Override
    @Transactional
    public UserResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = findUserById(userId);

        // 1. Upload ảnh mới — ném BusinessException nếu file không hợp lệ
        UploadResponse uploaded = cloudinaryService.uploadImage(file, UploadFolder.AVATAR);

        // 2. Xoá ảnh cũ (nếu có) — không ném exception để tránh block luồng chính
        String oldPublicId = user.getAvatarPublicId();
        if (oldPublicId != null && !oldPublicId.isBlank()) {
            cloudinaryService.deleteImage(oldPublicId);
            log.info("Deleted old avatar publicId={} for userId={}", oldPublicId, userId);
        }

        // 3. Cập nhật DB
        user.setAvatar(uploaded.getUrl());
        user.setAvatarPublicId(uploaded.getPublicId());

        User saved = userRepository.save(user);
        log.info("Avatar updated for userId={}, publicId={}", userId, uploaded.getPublicId());
        return userMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // ADMIN: LIST ALL USERS
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        Page<UserResponse> result = userRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(userMapper::toResponse);
        return PageResponse.of(result);
    }

    // ─────────────────────────────────────────────
    // ADMIN: TOGGLE STATUS
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = findUserById(userId);

        UserStatus next = switch (user.getStatus()) {
            case ACTIVE   -> UserStatus.INACTIVE;
            case INACTIVE -> UserStatus.ACTIVE;
            case BANNED   -> UserStatus.ACTIVE;
        };

        user.setStatus(next);
        userRepository.save(user);
        log.info("Admin toggled user {} status to {}", userId, next);
    }

    // ─────────────────────────────────────────────
    // getCurrentUserId — đọc từ SecurityContext
    // ─────────────────────────────────────────────

    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException("Không xác định được người dùng hiện tại");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email))
                .getId();
    }

    // ─────────────────────────────────────────────
    // INTERNAL HELPER
    // ─────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}