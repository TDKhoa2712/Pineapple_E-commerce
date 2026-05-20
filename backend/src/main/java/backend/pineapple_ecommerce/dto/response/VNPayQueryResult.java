package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VNPayQueryResult {
    private String responseCode;
    private String transactionStatus;
    private String message;
}