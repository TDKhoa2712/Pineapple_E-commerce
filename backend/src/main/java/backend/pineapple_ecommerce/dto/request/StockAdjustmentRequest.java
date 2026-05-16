package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Body cho API POST /api/v1/inventory/batches/{id}/adjust */
@Getter
@Setter
public class StockAdjustmentRequest {

    /**
     * Số lượng điều chỉnh: dương = thêm vào, âm = bớt đi.
     * Ví dụ: -5 = bớt 5 (hàng hỏng), +10 = thêm 10 (kiểm kê lại).
     */
    @NotNull(message = "Số lượng điều chỉnh không được để trống")
    private Integer adjustmentQty;

    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    private String reason;
}
