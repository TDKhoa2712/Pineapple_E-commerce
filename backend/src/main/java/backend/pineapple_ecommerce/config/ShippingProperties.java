package backend.pineapple_ecommerce.config;

import backend.pineapple_ecommerce.enums.CarrierCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình chung cho tính năng giao hàng.
 *
 * <p>application.yml:
 * <pre>
 * app:
 *   shipping:
 *     default-carrier: GHN          # Carrier mặc định khi không chỉ định
 *     sync-interval-minutes: 30     # Tần suất scheduler sync (phút)
 *     sync-delay-ms: 200            # Delay giữa các lần gọi API khi sync batch
 *     free-shipping-threshold: 500000  # Miễn phí ship khi đơn >= X VNĐ
 *     default-shipping-fee: 30000      # Phí ship mặc định
 *
 *   # Mỗi carrier có config riêng
 *   ghn:
 *     token: ${GHN_TOKEN}
 *     shop-id: ${GHN_SHOP_ID}
 *     base-url: https://dev-online-gateway.ghn.vn/shiip/public-api
 *
 *   # Thêm carrier mới chỉ cần thêm block config tương ứng
 *   ghtk:
 *     token: ${GHTK_TOKEN}
 *     base-url: https://services.giaohangtietkiem.vn
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.shipping")
public class ShippingProperties {

    /** Carrier sử dụng khi không chỉ định cụ thể */
    private CarrierCode defaultCarrier = CarrierCode.GHN;

    /** Khoảng cách giữa các lần sync batch (phút) */
    private int syncIntervalMinutes = 30;

    /** Delay giữa các request khi sync batch (ms) — tránh rate limit */
    private long syncDelayMs = 200;

    /** Giá trị đơn hàng được miễn phí ship (VNĐ) */
    private long freeShippingThreshold = 500_000;

    /** Phí ship mặc định khi chưa tính từ carrier (VNĐ) */
    private long defaultShippingFee = 30_000;
}