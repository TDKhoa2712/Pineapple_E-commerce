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
    // ĐƯỜNG DẪN 1: RETURN URL — VERIFY CHỮ KÝ → REDIRECT VỀ FRONTEND
    //
    // VNPay redirect trình duyệt khách về đây sau khi thanh toán.
    // Trước đây chỉ redirect blind mà không verify chữ ký —
    // attacker có thể forge "vnp_ResponseCode=00" để FE hiển thị
    // "thanh toán thành công" giả mạo.
    //
    // Sau khi sửa: service verify HMAC trước khi đọc bất kỳ param nào.
    // DB không được chạm ở đây — IPN đã cập nhật trước đó.
    // ─────────────────────────────────────────────────────────────

    @Operation(
            summary = "VNPay Return URL (verify chữ ký → redirect về Frontend)",
            description = "VNPay redirect trình duyệt khách về đây. " +
                    "Endpoint verify HMAC-SHA512 trước khi redirect — không ghi DB."
    )
    @GetMapping("/vnpay-return")
    public void vnpayReturn(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {

        // Truyền toàn bộ request vào service để service tự verify chữ ký.
        // KHÔNG đọc params ở đây rồi truyền raw string — service cần
        // toàn bộ request để verify HMAC.
        String redirectUrl = paymentService.buildReturnRedirectUrl(request);
        log.info("[Return URL] Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    // ─────────────────────────────────────────────────────────────
    // ĐƯỜNG DẪN 2: IPN WEBHOOK — NƠI DUY NHẤT GHI DATABASE
    // ─────────────────────────────────────────────────────────────

    @Operation(
            summary = "VNPay IPN Webhook (server-to-server)",
            description = "VNPay gọi ngầm để thông báo kết quả. " +
                    "Verify HMAC → kiểm tra số tiền → idempotency → cập nhật DB. " +
                    "Luôn trả HTTP 200 với RspCode trong body."
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
}