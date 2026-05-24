package backend.pineapple_ecommerce.modules.order.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productThumbnail;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private Long batchId;
    private String batchCode;
    private List<BatchAllocationResponse> batches;
}
