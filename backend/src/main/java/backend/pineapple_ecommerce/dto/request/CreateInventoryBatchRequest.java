package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateInventoryBatchRequest {

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    private Long farmId;

    @NotBlank(message = "Mã lô không được để trống")
    @Size(max = 50)
    private String batchCode;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer quantity;

    private LocalDate harvestDate;
    private LocalDate expiryDate;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "25.0")
    private BigDecimal sweetnessLevel;  // Độ Brix
}
