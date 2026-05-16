package backend.pineapple_ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps app.cors.* từ application-dev.yml / application-prod.yml
 *
 * application-dev.yml:
 *   app:
 *     cors:
 *       allowed-origins: ${FRONTEND_URL}
 *
 * Nếu cần nhiều origin (dev local), đặt trong .env:
 *   FRONTEND_URL=http://localhost:3000,http://localhost:5173
 * và cấu hình yml thành list:
 *   app:
 *     cors:
 *       allowed-origins:
 *         - ${FRONTEND_URL_1:http://localhost:3000}
 *         - ${FRONTEND_URL_2:http://localhost:5173}
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Danh sách origin được phép.
     * Hỗ trợ wildcard pattern (*, **.domain.com).
     */
    private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://localhost:5173"
    );
}