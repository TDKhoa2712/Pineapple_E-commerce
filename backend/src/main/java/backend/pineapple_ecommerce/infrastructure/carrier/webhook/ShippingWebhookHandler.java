package backend.pineapple_ecommerce.infrastructure.carrier.webhook;

import backend.pineapple_ecommerce.modules.shipping.dto.request.GhnWebhookRequest;
import backend.pineapple_ecommerce.common.enums.CarrierCode;
import backend.pineapple_ecommerce.modules.order.service.OrderService;
import backend.pineapple_ecommerce.modules.shipping.service.ShippingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Tag(name = "Webhooks", description = "Callback từ đơn vị vận chuyển")
@RestController
@RequestMapping("/api/v1/webhooks/shipping")
@RequiredArgsConstructor
public class ShippingWebhookHandler {

    private final ObjectMapper objectMapper;
    private final ShippingService shippingService;

    @Value("${app.ghn.webhook-secret:}")
    private String ghnWebhookSecret;

    @Operation(summary = "Webhook nhận trạng thái vận đơn từ carrier")
    @PostMapping("/{carrier}")
    public ResponseEntity<?> handleWebhook(
            @PathVariable String carrier,
            @RequestParam(value = "token", required = false) String token,
            @RequestHeader Map<String, String> headers,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payload JSON từ đơn vị vận chuyển (VD: GHN)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Mẫu GHN Webhook",
                                    value = "{\n  \"OrderCode\": \"Mã đơn của GHN\",\n  \"ClientOrderCode\": \"Order ID\",\n  \"Status\": \"delivering\",\n  \"Type\": \"Switch_status\"\n}"
                            )
                    )
            )
            @RequestBody String rawPayload) {

        log.info("Webhook received from {}. Payload: {}", carrier, rawPayload);

        try {
            CarrierCode carrierCode = CarrierCode.valueOf(carrier.toUpperCase());

            // 1. Xác thực bảo mật
            if (!verifySecurity(carrierCode, token, headers, rawPayload)) {
                log.warn("Cảnh báo bảo mật: Yêu cầu Webhook không hợp lệ từ {}", carrier);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Unauthorized webhook access"));
            }

            // ==========================================
            // 2. XỬ LÝ RIÊNG CHO GHN
            // ==========================================
            if (carrierCode == CarrierCode.GHN) {
                GhnWebhookRequest ghnPayload = objectMapper.readValue(rawPayload, GhnWebhookRequest.class);

                if ("Switch_status".equals(ghnPayload.getType())) {
                    // SỬA LẠI TẠI ĐÂY:
                    // Gọi shippingService.syncStatus để cập nhật ĐỒNG THỜI bảng Shipment và Order
                    shippingService.syncStatus(ghnPayload.getOrderCode(), CarrierCode.GHN);
                }

                return ResponseEntity.ok(Map.of("code", 200, "message", "Success"));
            }

            // ==========================================
            // 3. XỬ LÝ CHO CÁC HÃNG KHÁC (GHTK, Viettel Post...)
            // ==========================================
            Map<String, Object> payloadMap = objectMapper.readValue(rawPayload, Map.class);
            String externalOrderCode = extractOrderCode(carrierCode, payloadMap);

            if (externalOrderCode == null || externalOrderCode.isBlank()) {
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "missing_order_code"));
            }

            shippingService.syncStatus(externalOrderCode, carrierCode);
            return ResponseEntity.ok(Map.of("status", "ok"));

        } catch (IllegalArgumentException e) {
            log.error("Không hỗ trợ hãng vận chuyển: {}", carrier);
            return ResponseEntity.badRequest().body(Map.of("error", "Unsupported carrier"));
        } catch (Exception e) {
            log.error("Lỗi xử lý Webhook cho hãng {}: {}", carrier, e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private String getHeaderCaseInsensitive(Map<String, String> headers, String key) {
        if (headers == null) return null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Thuật toán kiểm tra bảo mật tùy theo thiết kế API của từng đơn vị vận chuyển.
     */
    private boolean verifySecurity(CarrierCode carrierCode, String token, Map<String, String> headers, String rawPayload) {
        return switch (carrierCode) {
            case GHN -> {
                String headerToken = getHeaderCaseInsensitive(headers, "X-Pushback-Token");
                if (headerToken == null) {
                    headerToken = getHeaderCaseInsensitive(headers, "Token");
                }
                yield ghnWebhookSecret != null && !ghnWebhookSecret.isBlank() && ghnWebhookSecret.equals(headerToken);
            }
//            case GHTK -> {
//                // Ví dụ sau này GHTK có hỗ trợ HMAC Hash qua Header
//                String signature = headers.get("x-ghtk-hash");
//                String ghtkSecret = "thu_nghiem_secret_ghtk";
//                yield signature != null && isValidHmac(rawPayload, ghtkSecret, signature);
//            }
            // Mặc định tạm đóng hoặc mở tùy chiến lược tích hợp
            default -> false;
        };
    }

    /**
     * Hàm băm HMAC SHA256 chuẩn để so khớp.
     */
    private boolean isValidHmac(String payload, String secret, String expectedSignature) {
        if (secret == null || secret.isBlank()) {
            log.error("Chưa cấu hình Webhook Secret Key trong hệ thống.");
            return false;
        }
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);

            byte[] hash = sha256HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Chuyển đổi byte array thành chuỗi Hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equalsIgnoreCase(expectedSignature);
        } catch (Exception e) {
            log.error("Lỗi khi tính toán HMAC: {}", e.getMessage());
            return false;
        }
    }

    private String extractOrderCode(CarrierCode carrierCode, Map<String, Object> payload) {
        return switch (carrierCode) {
            case GHN -> (String) payload.get("order_code");
            case GHTK -> (String) payload.getOrDefault("partner_id", payload.get("label_id"));
            case VIETTEL_POST -> (String) payload.get("ORDER_NUMBER");
            case J_AND_T -> (String) payload.get("billcode");
            case BEST_EXPRESS -> (String) payload.get("mailno");
        };
    }
}