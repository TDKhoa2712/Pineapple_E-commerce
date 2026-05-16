package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Kết quả sau khi điều chỉnh tồn kho — dùng cho /inventory/batches/{id}/adjust */
@Getter
@Builder
public class StockAdjustmentResponse {
    private Long          id;
    private Long          batchId;
    private String        batchCode;
    private Long          productId;
    private String        productName;
    private Integer       adjustmentQty;
    private String        reason;
    private Integer       qtyBefore;
    private Integer       qtyAfter;
    private String        adjustedByName;
    private LocalDateTime createdAt;
}
