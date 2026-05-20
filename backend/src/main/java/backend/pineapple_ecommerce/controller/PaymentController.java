package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.PaymentResponse;
import backend.pineapple_ecommerce.security.CustomUserDetails;
import backend.pineapple_ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Payments", description = "Xử lý thanh toán VNPAY/MoMo")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ─────────────────────────────────────────────────────────────
    // KHỞI TẠO THANH TOÁN
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Khởi tạo thanh toán VNPAY cho đơn hàng")
    @PostMapping("/{orderId}/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {

        PaymentResponse response = paymentService.initiatePayment(
                orderId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─────────────────────────────────────────────────────────────
    // ĐƯỜNG DẪN 1: RETURN URL — CHỈ REDIRECT VỀ FRONTEND
    // ─────────────────────────────────────────────────────────────

    @Operation(
            summary = "VNPAY Return URL (redirect về Frontend)",
            description = "VNPAY redirect trình duyệt khách về đây sau khi thanh toán. "
                    + "Endpoint này CHỈ redirect về Frontend, không ghi DB."
    )
    @GetMapping("/vnpay-return")
    public void vnpayReturn(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {

        Map<String, String> params = extractParams(request);
        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.getOrDefault("vnp_TxnRef", "");

        log.info("[Return URL] txnRef={}, responseCode={}", txnRef, responseCode);

        // Chỉ redirect — không chạm database
        String redirectUrl = paymentService.buildReturnRedirectUrl(responseCode, txnRef);
        response.sendRedirect(redirectUrl);
    }

    // ─────────────────────────────────────────────────────────────
    // ĐƯỜNG DẪN 2: IPN WEBHOOK — NƠI DUY NHẤT GHI DATABASE
    // ─────────────────────────────────────────────────────────────

    @Operation(
            summary = "VNPAY IPN Webhook (server-to-server)",
            description = "VNPAY gọi ngầm để thông báo kết quả thanh toán. "
                    + "Đây là nơi DUY NHẤT cập nhật trạng thái đơn hàng."
    )
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> handleIpn(HttpServletRequest request) {
        return ResponseEntity.ok(
                paymentService.handleVnpayIpn(request)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY PAYMENT
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy thông tin thanh toán của đơn hàng")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.success(paymentService.getPaymentByOrderId(orderId)));
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                fields.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return fields;
    }
}
