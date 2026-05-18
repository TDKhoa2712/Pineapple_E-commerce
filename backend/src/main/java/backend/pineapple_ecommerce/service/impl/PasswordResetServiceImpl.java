package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.entity.OtpToken;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.OtpType;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.repository.OtpTokenRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.EmailService;
import backend.pineapple_ecommerce.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation luồng reset mật khẩu qua OTP.
 *
 * <p>Bảo mật:
 * <ul>
 *   <li>SecureRandom (cryptographically strong) thay vì Random</li>
 *   <li>OTP hết hạn sau 10 phút</li>
 *   <li>Xoá OTP cũ trước khi tạo mới — tránh nhiều OTP active cùng lúc</li>
 *   <li>initiateReset() không tiết lộ email có tồn tại không (anti-enumeration)</li>
 *   <li>Sau reset thành công, OTP đánh dấu used = true</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int    OTP_EXPIRY_MINUTES = 10;
    private static final int    OTP_LENGTH         = 6;

    private final UserRepository    userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService      emailService;
    private final PasswordEncoder   passwordEncoder;

    // ─────────────────────────────────────────────
    // INITIATE RESET
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void initiateReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Anti-enumeration: không tiết lộ email có tồn tại hay không
        // Log internal để debug nếu cần
        if (userOpt.isEmpty()) {
            log.warn("[PasswordReset] initiateReset for unknown email: {}", email);
            return; // trả về thành công giả
        }

        User user = userOpt.get();

        // Xoá OTP cũ trước khi tạo mới
        otpTokenRepository.deleteAllByUserIdAndType(user.getId(), OtpType.PASSWORD_RESET);

        String otp = generateOtp();

        OtpToken token = OtpToken.builder()
                .user(user)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .build();

        otpTokenRepository.save(token);

        // Email gửi async — không block transaction
        emailService.sendPasswordResetOtp(email, otp);

        log.info("[PasswordReset] OTP created for userId={}", user.getId());
    }

    // ─────────────────────────────────────────────
    // RESET PASSWORD
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Email không hợp lệ"));

        OtpToken token = otpTokenRepository
                .findValidOtp(user.getId(), otp, OtpType.PASSWORD_RESET, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(
                        "Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu mã mới."));

        // Đặt mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Vô hiệu hoá OTP sau khi dùng
        token.setUsed(true);
        otpTokenRepository.save(token);

        log.info("[PasswordReset] Password reset successfully for userId={}", user.getId());
    }

    // ─────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────

    /**
     * Tạo OTP 6 chữ số dùng SecureRandom (cryptographically strong).
     * Đảm bảo luôn đủ 6 chữ số kể cả khi số < 100000.
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // [100000, 999999]
        return String.valueOf(otp);
    }
}
