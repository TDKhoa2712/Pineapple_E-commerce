package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Payments", description = "Xử lý thanh toán VNPAY/MoMo")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // VNPAY sẽ GET về endpoint này sau khi user thanh toán xong
    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<String>> vnpayReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            fields.put(entry.getKey(), entry.getValue()[0]);
        }

        // Gọi hàm xử lý callback trong PaymentService
        paymentService.handlePaymentCallback("VNPAY", fields);

        // Trong thực tế, bạn có thể redirect về 1 trang Success/Failed của ReactJS/VueJS ở đây
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý kết quả thanh toán"));
    }
}