package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Tổng hợp tồn kho một sản phẩm — dùng cho /inventory/summary */
@Getter
@Builder
public class InventorySummaryResponse {
    private Long    productId;
    private String  productName;
    private Integer totalStock;
    private Long    batchCount;
}
