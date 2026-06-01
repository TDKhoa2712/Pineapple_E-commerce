package backend.pineapple_ecommerce.modules.auth.service;

import backend.pineapple_ecommerce.infrastructure.email.EmailService;
import backend.pineapple_ecommerce.modules.auth.models.OtpToken;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.enums.AuthProvider;
import backend.pineapple_ecommerce.common.enums.OtpType;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.modules.auth.repository.OtpTokenRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int OTP_EXPIRY_MINUTES   = 10;
    private static final int MAX_RESEND_COUNT     = 3;
    private static final int RESEND_WINDOW_MINUTES = 10;

    private final UserRepository     userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;

    // ─────────────────────────────────────────────
    // SEND VERIFICATION OTP (nội bộ, gọi sau register)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void sendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản: " + email));

        // Chỉ gửi cho LOCAL user chưa verify
        if (!AuthProvider.LOCAL.equals(user.getProvider())) {
            log.debug("[EmailVerify] Bỏ qua — OAuth2 user không cần verify: {}", email);
            return;
        }
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.debug("[EmailVerify] Bỏ qua — email đã được xác thực: {}", email);
            return;
        }

        String otp = createAndSaveOtp(user);
        emailService.sendEmailVerificationOtp(email, otp);

        log.info("[EmailVerify] OTP xác thực email đã gửi cho userId={}", user.getId());
    }

    // ─────────────────────────────────────────────
    // VERIFY EMAIL
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void verifyEmail(String email, String otp) {
        log.info("verify emial run service impl");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Email không hợp lệ"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException("Email này đã được xác thực rồi");
        }

        OtpToken token = otpTokenRepository
                .findValidOtp(user.getId(), otp, OtpType.EMAIL_VERIFICATION, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(
                        "Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu mã mới."));

        // Đánh dấu email đã xác thực
        user.setEmailVerified(true);
        userRepository.save(user);

        // Vô hiệu hoá OTP sau khi dùng (audit trail)
        token.setUsed(true);
        otpTokenRepository.save(token);

        log.info("[EmailVerify] Email xác thực thành công cho userId={}", user.getId());
    }

    // ─────────────────────────────────────────────
    // RESEND VERIFICATION OTP
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void resendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản với email này"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException("Email này đã được xác thực rồi");
        }

        if (!AuthProvider.LOCAL.equals(user.getProvider())) {
            throw new BusinessException("Tài khoản OAuth2 không cần xác thực email");
        }

        // Rate limit: kiểm tra số lần gửi trong cửa sổ thời gian
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RESEND_WINDOW_MINUTES);
        long sentCount = otpTokenRepository.countOtpSentSince(
                user.getId(), OtpType.EMAIL_VERIFICATION, windowStart);

        if (sentCount >= MAX_RESEND_COUNT) {
            throw new BusinessException(
                    "Bạn đã yêu cầu gửi lại OTP quá " + MAX_RESEND_COUNT + " lần trong " +
                            RESEND_WINDOW_MINUTES + " phút. Vui lòng thử lại sau.");
        }

        String otp = createAndSaveOtp(user);
        emailService.sendEmailVerificationOtp(email, otp);

        log.info("[EmailVerify] OTP đã gửi lại cho userId={}, lần thứ={}", user.getId(), sentCount + 1);
    }

    // ─────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────

    /**
     * Xoá OTP cũ (cùng type), tạo OTP mới, lưu DB và trả về mã OTP.
     */
    private String createAndSaveOtp(User user) {
        // Xoá OTP cũ cùng type để đảm bảo chỉ có 1 OTP active
        otpTokenRepository.deleteAllByUserIdAndType(user.getId(), OtpType.EMAIL_VERIFICATION);

        String otp = generateOtp();

        OtpToken token = OtpToken.builder()
                .user(user)
                .otp(otp)
                .type(OtpType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .build();

        otpTokenRepository.save(token);
        return otp;
    }

    /**
     * Tạo OTP 6 chữ số dùng SecureRandom (cryptographically strong).
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // [100000, 999999]
        return String.valueOf(otp);
    }
}