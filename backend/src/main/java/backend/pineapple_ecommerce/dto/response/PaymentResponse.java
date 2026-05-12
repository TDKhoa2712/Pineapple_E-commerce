package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String provider;
    private String transactionCode;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;

    // Dùng khi redirect đến cổng thanh toán
    private String paymentUrl;
}
