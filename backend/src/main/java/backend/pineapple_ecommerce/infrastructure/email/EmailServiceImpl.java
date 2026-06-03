package backend.pineapple_ecommerce.infrastructure.email;

import backend.pineapple_ecommerce.infrastructure.email.config.MailProperties;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Implementation gửi email HTML thông qua JavaMailSender + Thymeleaf.
 *
 * <p>Thiết kế quan trọng:
 * <ul>
 *   <li>@Async("emailTaskExecutor") — chạy trên thread pool riêng, không block request</li>
 *   <li>@Retryable — tự retry tối đa 3 lần nếu SMTP lỗi tạm thời,
 *       với exponential backoff (2s → 4s → 8s)</li>
 *   <li>MimeMessage multipart — gửi cả text/plain lẫn text/html để tránh bị spam filter</li>
 *   <li>Mọi exception đều được log đầy đủ context; không ném ra ngoài để tránh
 *       ảnh hưởng luồng nghiệp vụ chính</li>
 * </ul>
 *
 * <p>Dependency cần thêm vào pom.xml:
 * <pre>
 *   spring-boot-starter-mail
 *   spring-boot-starter-thymeleaf
 *   spring-retry
 *   spring-aspects (bắt buộc đi kèm spring-retry)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender  mailSender;
    private final TemplateEngine  templateEngine;
    private final MailProperties  mailProperties;

    // ─────────────────────────────────────────────
    // WELCOME EMAIL
    // ─────────────────────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    @Retryable(retryFor = MailException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("fullName", fullName);

            send(toEmail,
                 "Chào mừng bạn đến với Pineapple E-commerce! 🍍",
                 "email/welcome",
                 ctx,
                 buildWelcomePlainText(fullName));

            log.info("[Email] Welcome sent to {}", toEmail);
        } catch (Exception ex) {
            log.error("[Email] Failed to send welcome to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────
    // PASSWORD RESET OTP
    // ─────────────────────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    @Retryable(retryFor = MailException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendPasswordResetOtp(String toEmail, String otp) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("otp", otp);
            ctx.setVariable("expiryMinutes", 10);

            send(toEmail,
                 "[Pineapple] Mã OTP đặt lại mật khẩu",
                 "email/password-reset-otp",
                 ctx,
                 buildOtpPlainText(otp));

            log.info("[Email] OTP sent to {}", toEmail);
        } catch (Exception ex) {
            log.error("[Email] Failed to send OTP to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    @Override
    @Async("emailTaskExecutor")
    @Retryable(retryFor = MailException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendEmailVerificationOtp(String toEmail, String otp) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("otp", otp);
            ctx.setVariable("expiryMinutes", 10);

            send(toEmail,
                    "[Pineapple] Mã xác thực email của bạn",
                    "email/email-verification-otp",   // Thymeleaf template
                    ctx,
                    buildEmailVerificationPlainText(otp));

            log.info("[Email] Email verification OTP sent to {}", toEmail);
        } catch (Exception ex) {
            log.error("[Email] Failed to send verification OTP to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    @Recover
    public void recoverEmailVerificationOtp(MailException ex, String toEmail, String otp) {
        log.error("[Email] All retries failed for verification OTP to {}: {}", toEmail, ex.getMessage());
    }

    private String buildEmailVerificationPlainText(String otp) {
        return """
                Xác thực email Pineapple E-commerce
                ────────────────────────────────────
                Mã xác thực của bạn: %s
                
                Mã có hiệu lực trong 10 phút.
                Nếu bạn không tạo tài khoản, hãy bỏ qua email này.
                
                Trân trọng,
                Pineapple E-commerce
                """.formatted(otp);
    }

    // ─────────────────────────────────────────────
    // ORDER CONFIRMATION
    // ─────────────────────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    @Retryable(retryFor = MailException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendOrderConfirmation(String toEmail, OrderResponse order) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("order", order);

            send(toEmail,
                 "Xác nhận đơn hàng #" + order.getId() + " | Pineapple",
                 "email/order-confirmation",
                 ctx,
                 buildOrderConfirmPlainText(order));

            log.info("[Email] Order confirmation sent to {} for orderId={}", toEmail, order.getId());
        } catch (Exception ex) {
            log.error("[Email] Failed to send order confirmation to {} orderId={}: {}",
                      toEmail, order.getId(), ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────
    // ORDER STATUS UPDATE
    // ─────────────────────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    @Retryable(retryFor = MailException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendOrderStatusUpdate(String toEmail, OrderResponse order, String newStatus) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("order", order);
            ctx.setVariable("newStatus", newStatus);

            send(toEmail,
                 "Cập nhật đơn hàng #" + order.getId() + " — " + newStatus,
                 "email/order-status-update",
                 ctx,
                 "Đơn hàng #" + order.getId() + " của bạn hiện đang ở trạng thái: " + newStatus);

            log.info("[Email] Status update '{}' sent to {} for orderId={}", newStatus, toEmail, order.getId());
        } catch (Exception ex) {
            log.error("[Email] Failed to send status update to {} orderId={}: {}",
                      toEmail, order.getId(), ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────
    // FARM APPROVAL RESULT
    // ─────────────────────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    @Retryable(retryFor = MailException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendFarmApprovalResult(String toEmail, String farmName,
                                       boolean approved, String rejectionReason) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("farmName", farmName);
            ctx.setVariable("approved", approved);
            ctx.setVariable("rejectionReason", rejectionReason);

            String subject = approved
                    ? "🎉 Farm \"" + farmName + "\" đã được duyệt | Pineapple"
                    : "Kết quả xét duyệt Farm \"" + farmName + "\" | Pineapple";

            send(toEmail, subject, "email/farm-approval", ctx,
                 buildFarmApprovalPlainText(farmName, approved, rejectionReason));

            log.info("[Email] Farm approval ({}) sent to {} for farm='{}'",
                     approved ? "APPROVED" : "REJECTED", toEmail, farmName);
        } catch (Exception ex) {
            log.error("[Email] Failed to send farm approval to {} farm='{}': {}",
                      toEmail, farmName, ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPER — Core send logic
    // ─────────────────────────────────────────────

    /**
     * Gửi MimeMessage multipart (text/plain + text/html).
     * Dùng text/plain làm fallback cho email client không hỗ trợ HTML.
     */
    private void send(String toEmail, String subject, String templateName, Context ctx, String plainText) {

        try {
            ctx.setVariable("baseUrl", mailProperties.getBaseUrl());

            String htmlContent = templateEngine.process(templateName, ctx);

            MimeMessage mime = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(
                    mailProperties.getFromAddress(),
                    mailProperties.getFromName()
            );

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, htmlContent);

            mailSender.send(mime);

        } catch (Exception ex) {

            log.error("[Email] Failed sending email to={} subject={}", toEmail, subject, ex);

            throw new org.springframework.mail.MailSendException("Failed to send email", ex);
        }
    }

    // ─────────────────────────────────────────────
    // PLAIN TEXT FALLBACKS
    // ─────────────────────────────────────────────

    private String buildWelcomePlainText(String fullName) {
        return """
                Xin chào %s,
                Chào mừng bạn đến với Pineapple E-commerce!
                Tài khoản của bạn đã được tạo thành công.
                Hãy khám phá các sản phẩm nông sản tươi ngon tại pineapple.vn
                
                Trân trọng,
                Đội ngũ Pineapple
                """.formatted(fullName);
    }

    private String buildOtpPlainText(String otp) {
        return """
                Mã OTP đặt lại mật khẩu của bạn là: %s
                Mã có hiệu lực trong 10 phút.
                Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                
                Trân trọng,
                Đội ngũ Pineapple
                """.formatted(otp);
    }

    private String buildOrderConfirmPlainText(OrderResponse order) {
        return """
                Cảm ơn bạn đã đặt hàng tại Pineapple!
                Mã đơn hàng: #%d
                Tổng tiền: %s VNĐ
                Phương thức thanh toán: %s
                Địa chỉ giao hàng: %s
                
                Chúng tôi sẽ xử lý đơn hàng và thông báo cho bạn sớm nhất.
                
                Trân trọng,
                Đội ngũ Pineapple
                """.formatted(
                order.getId(),
                order.getTotalAmount().toPlainString(),
                order.getPaymentMethod(),
                order.getShippingAddress());
    }

    private String buildFarmApprovalPlainText(String farmName, boolean approved, String reason) {
        if (approved) {
            return """
                    Farm "%s" của bạn đã được duyệt thành công!
                    Bạn có thể bắt đầu thêm sản phẩm và quản lý trang trại tại pineapple.vn
                    
                    Trân trọng,
                    Đội ngũ Pineapple
                    """.formatted(farmName);
        } else {
            return """
                    Farm "%s" của bạn chưa được duyệt.
                    Lý do: %s
                    
                    Vui lòng cập nhật thông tin và gửi lại đề nghị duyệt.
                    
                    Trân trọng,
                    Đội ngũ Pineapple
                    """.formatted(farmName, reason != null ? reason : "Không có lý do cụ thể");
        }
    }

    @Recover
    public void recover(
            MailException ex,
            String toEmail,
            String fullName
    ) {

        log.error(
                "[Email] FINAL FAILURE sending welcome email to {}",
                toEmail,
                ex
        );
    }
}
