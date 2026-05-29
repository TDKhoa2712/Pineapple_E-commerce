package backend.pineapple_ecommerce.modules.user.service;

import backend.pineapple_ecommerce.modules.user.mapper.UserMapper;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.common.enums.UserStatus;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.modules.auth.dto.request.AdminResetPasswordRequest;
import backend.pineapple_ecommerce.modules.auth.dto.request.ChangePasswordRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateProfileRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateUserRolesRequest;
import backend.pineapple_ecommerce.modules.user.dto.request.UpdateUserStatusRequest;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import backend.pineapple_ecommerce.modules.user.dto.response.UserResponse;
import backend.pineapple_ecommerce.modules.auth.models.Role;
import backend.pineapple_ecommerce.common.enums.RoleName;
import backend.pineapple_ecommerce.common.enums.UploadFolder;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.modules.auth.repository.RoleRepository;
import backend.pineapple_ecommerce.infrastructure.cloudinary.CloudinaryService;
import backend.pineapple_ecommerce.modules.auth.service.RefreshTokenService;
import backend.pineapple_ecommerce.common.util.FileValidator;
import backend.pineapple_ecommerce.modules.user.specification.UserSpecification;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import backend.pineapple_ecommerce.security.CustomUserDetails;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository    roleRepository;
    private final UserMapper userMapper;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder   passwordEncoder;
    private final FileValidator fileValidator;
    private final RefreshTokenService refreshTokenService;

    // ─────────────────────────────────────────────
    // USER — TỰ QUẢN LÝ TÀI KHOẢN
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return userMapper.toResponse(findUserById(getCurrentUserId()));
    }

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

    @Override
    @Transactional
    public UserResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Validate file
        fileValidator.validateImage(file);

        // Xóa avatar cũ nếu tồn tại
        if (user.getAvatar() != null && !user.getAvatar().isBlank()) {
            cloudinaryService.deleteImage(user.getAvatar()); // giả sử publicId được lưu hoặc xử lý riêng
        }

        // Upload avatar mới
        UploadResponse uploadResponse = cloudinaryService.uploadImage(file, UploadFolder.AVATAR);

        user.setAvatar(uploadResponse.getUrl());
        userRepository.save(user);

        log.info("User {} uploaded new avatar", userId);
        return userMapper.toResponse(user);
    }

    // ─────────────────────────────────────────────
    // ĐỔI MẬT KHẨU (user tự thực hiện)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        // 1. Xác nhận mật khẩu cũ
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không chính xác");
        }

        // 2. newPassword và confirmNewPassword phải khớp
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // 3. Mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeByUserId(user.getId());
        log.info("User {} changed their password", userId);
    }

    // ─────────────────────────────────────────────
    // ADMIN — XEM USER
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return userMapper.toResponse(findUserById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size,
                                                  UserStatus status, String keyword) {
        Specification<User> spec = Specification.allOf(
                UserSpecification.hasStatus(status),
                UserSpecification.searchByKeyword(keyword)
        );

        Page<UserResponse> result = userRepository
                .findAll(spec, PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .map(userMapper::toResponse);
        return PageResponse.of(result);
    }

    // ─────────────────────────────────────────────
    // ADMIN — CẬP NHẬT TRẠNG THÁI
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long targetUserId, Long adminId,
                                         UpdateUserStatusRequest request) {
        // Admin không thể tự khoá chính mình
        if (targetUserId.equals(adminId)) {
            throw new BusinessException("Bạn không thể thay đổi trạng thái tài khoản của chính mình");
        }

        User user = findUserById(targetUserId);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(request.getStatus());

        User saved = userRepository.save(user);
        log.info("Admin {} changed user {} status: {} → {} | reason: {}",
                adminId, targetUserId, oldStatus, request.getStatus(),
                request.getReason() != null ? request.getReason() : "N/A");

        return userMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // ADMIN — PHÂN QUYỀN ROLE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponse updateUserRoles(Long targetUserId, Long adminId,
                                        UpdateUserRolesRequest request) {
        // Admin không thể tự gỡ role ADMIN của chính mình
        if (targetUserId.equals(adminId) && !request.getRoles().contains(RoleName.ROLE_ADMIN)) {
            throw new BusinessException("Bạn không thể tự gỡ quyền ADMIN của chính mình");
        }

        User user = findUserById(targetUserId);

        // Resolve các Role entity từ DB
        Set<Role> newRoles = new HashSet<>();

        // ROLE_USER luôn được đảm bảo — ngay cả khi admin không gửi lên
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.ROLE_USER));
        newRoles.add(userRole);

        // Thêm các role admin chỉ định
        for (RoleName roleName : request.getRoles()) {
            if (roleName == RoleName.ROLE_USER) continue;

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
            newRoles.add(role);
        }

        Set<String> oldRoleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(java.util.stream.Collectors.toSet());

        user.getRoles().clear();
        user.getRoles().addAll(newRoles);

        User saved = userRepository.save(user);

        Set<String> newRoleNames = newRoles.stream()
                .map(r -> r.getName().name())
                .collect(java.util.stream.Collectors.toSet());
        log.info("Admin {} updated roles for user {}: {} → {}",
                adminId, targetUserId, oldRoleNames, newRoleNames);

        return userMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // ADMIN — ĐẶT LẠI MẬT KHẨU
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void adminResetPassword(Long targetUserId, AdminResetPasswordRequest request) {
        User user = findUserById(targetUserId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeByUserId(user.getId());
        log.info("Admin reset password for userId={}", targetUserId);
    }

    // ─────────────────────────────────────────────
    // getCurrentUserId
    // ─────────────────────────────────────────────

    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException("Không xác định được người dùng hiện tại");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUserId();
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email))
                .getId();
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        log.info("Get user by email: {}", email);
        return userRepository.findByEmail(email).map(userMapper::toResponse)
                .orElseThrow(()-> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    public User getEntityUser(Long userId) {
        return findUserById(userId);
    }

    // ─────────────────────────────────────────────
    // INTERNAL HELPER
    // ─────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}