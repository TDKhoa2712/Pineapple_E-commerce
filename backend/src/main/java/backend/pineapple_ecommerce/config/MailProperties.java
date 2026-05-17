package backend.pineapple_ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ánh xạ cấu hình app.mail.* từ application.yml / application-dev.yml
 *
 * Cấu hình SMTP (spring.mail.*) được để riêng ở application.yml
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Địa chỉ email gửi đi */
    private String fromAddress = "pineappleecommerce.example@gmail.com";

    /** Tên hiển thị của người gửi */
    private String fromName = "Pineapple E-commerce";

    /** Base URL của frontend/website - dùng để tạo link trong email */
    private String baseUrl = "http://localhost:3000";

    /** Email hỗ trợ (dùng trong footer email) */
    private String supportEmail = "support@pineapple.vn";
}