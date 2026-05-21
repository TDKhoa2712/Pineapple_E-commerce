package backend.pineapple_ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình GHN API lấy từ application.yml.
 *
 * <p>Cần thêm vào application-dev.yml / application-prod.yml:
 * <pre>
 * app:
 *   ghn:
 *     token: ${GHN_TOKEN}
 *     shop-id: ${GHN_SHOP_ID}
 *     base-url: https://dev-online-gateway.ghn.vn/shiip/public-api   # dev
 *     # base-url: https://online-gateway.ghn.vn/shiip/public-api     # prod
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ghn")
public class GhnProperties {

    /** Token xác thực GHN — bắt buộc */
    private String token;

    /** Shop ID GHN — bắt buộc cho Calculate Fee và Create Order */
    private Integer shopId;

    /**
     * Base URL GHN API.
     * Dev : https://dev-online-gateway.ghn.vn/shiip/public-api
     * Prod: https://online-gateway.ghn.vn/shiip/public-api
     */
    private String baseUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api";
}
