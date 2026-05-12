package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class InventoryBatchResponse {
    private Long       id;
    private Long       productId;
    private String     productName;
    private Long       farmId;
    private String     farmName;
    private String     batchCode;
    private Integer    quantity;
    private Integer    remainingQuantity;
    private LocalDate  harvestDate;
    private LocalDate  expiryDate;
    private BigDecimal sweetnessLevel;
    private String     status;
}
