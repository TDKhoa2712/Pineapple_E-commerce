package backend.pineapple_ecommerce.modules.order.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchAllocationResponse {
    private Long batchId;
    private String batchCode;
    private Integer quantity;
}
