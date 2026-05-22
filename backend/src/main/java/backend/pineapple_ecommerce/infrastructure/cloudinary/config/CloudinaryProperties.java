package backend.pineapple_ecommerce.infrastructure.cloudinary.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Maps app.cloudinary.* từ application-dev.yml / application-prod.yml
 *
 * application.yml:
 *   app:
 *     cloudinary:
 *       cloud-name: ${CLOUDINARY_CLOUD_NAME}
 *       api-key:    ${CLOUDINARY_API_KEY}
 *       api-secret: ${CLOUDINARY_API_SECRET}
 *       secure: true
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cloudinary")
public class CloudinaryProperties {

    private String  cloudName;
    private String  apiKey;
    private String  apiSecret;
    private boolean secure = true;
}