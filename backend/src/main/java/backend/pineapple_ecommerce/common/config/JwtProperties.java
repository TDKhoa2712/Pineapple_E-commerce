package backend.pineapple_ecommerce.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Maps jwt.* properties from application.yml.
 *
 * application.yml example:
 * jwt:
 *   secret: your-256-bit-secret-key-here-change-in-production
 *   access-token-expiration-ms: 900000       # 15 minutes
 *   refresh-token-expiration-ms: 604800000   # 7 days
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationMs = 900_000L;         // 15 min default
    private long refreshTokenExpirationMs = 604_800_000L;    // 7 days default
}
