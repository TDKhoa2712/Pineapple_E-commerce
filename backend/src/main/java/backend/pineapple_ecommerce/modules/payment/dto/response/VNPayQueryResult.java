package backend.pineapple_ecommerce.modules.payment.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VNPayQueryResult {
    private String responseCode;
    private String transactionStatus;
    private String rawResponse;
    private String message;
}