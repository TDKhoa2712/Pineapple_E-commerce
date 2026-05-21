package backend.pineapple_ecommerce.webhook;

import backend.pineapple_ecommerce.config.GhnProperties;
import backend.pineapple_ecommerce.service.GhnShippingService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Nhận callback (webhook) từ GHN khi trạng thái vận đơn thay đổi.
 *
 * <p>Cấu hình webhook trên GHN Merchant Portal:
 *   URL: https://yourdomain.com/api/v1/webhooks/ghn
 *   (Dev: dùng ngrok như VNPay: https://xxxx.ngrok-free.dev/api/v1/webhooks/ghn)
 *
 * <p>Luồng:
 * 1. GHN POST về endpoint này mỗi khi trạng thái đơn thay đổi
 * 2. Handler gọi GhnShippingService.syncStatusFromGhn(orderCode)
 * 3. Service cập nhật GhnShipment.currentStatus + Order.status
 * 4. Trả 200 OK cho GHN (nếu không trả 200, GHN sẽ retry)
 *
 * <p>Bảo mật:
 * Endpoint này PUBLIC (không cần JWT).
 * Cần thêm vào SecurityConfig PUBLIC_POST list.
 * Nên validate thêm GHN Token header nếu muốn tăng bảo mật.
 */
@Slf4j
@Tag(name = "Webhooks", description = "Callback từ GHN")
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class GhnWebhookHandler {

    private final GhnShippingService shippingService;
    private final GhnProperties ghnProperties;

    @Operation(summary = "GHN Webhook — nhận callback trạng thái vận đơn")
    @PostMapping("/ghn")
    public ResponseEntity<Map<String, String>> handleGhnCallback(
            @RequestBody GhnCallbackPayload payload) {

        log.info("GHN Webhook received: orderCode={}, status={}, clientOrderCode={}",
                payload.getOrderCode(), payload.getStatus(), payload.getClientOrderCode());

        try {
            String orderCode = payload.getOrderCode();
            if (orderCode == null || orderCode.isBlank()) {
                log.warn("GHN webhook: missing order_code in payload");
                return ResponseEntity.ok(Map.of("status", "ignored"));
            }

            shippingService.syncStatusFromGhn(orderCode);

            return ResponseEntity.ok(Map.of("status", "ok"));

        } catch (Exception e) {
            // QUAN TRỌNG: Luôn trả 200 để GHN không retry liên tục
            // Log lỗi để debug sau
            log.error("GHN webhook processing failed for orderCode={}: {}",
                    payload.getOrderCode(), e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * GHN Webhook payload.
     * Ref: https://api.ghn.vn/home/docs/detail?id=47
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GhnCallbackPayload {

        @JsonProperty("order_code")
        private String orderCode;

        /** Mã đơn nội bộ của shop (ta gửi khi tạo vận đơn) */
        @JsonProperty("client_order_code")
        private String clientOrderCode;

        /** Trạng thái mới: "delivering", "delivered", "delivery_fail", ... */
        private String status;

        /** Thời gian cập nhật */
        @JsonProperty("updated_date")
        private String updatedDate;

        /** Lý do giao thất bại (nếu status = delivery_fail) */
        @JsonProperty("reason")
        private String reason;

        /** COD amount đã thu */
        @JsonProperty("cod_amount")
        private Integer codAmount;

        @JsonProperty("shop_id")
        private Integer shopId;
    }
}
