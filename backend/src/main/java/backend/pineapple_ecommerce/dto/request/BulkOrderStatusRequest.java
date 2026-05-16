package backend.pineapple_ecommerce.dto.request;

import backend.pineapple_ecommerce.enums.OrderStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Body cho API POST /api/v1/orders/admin/bulk-status */
@Getter
@Setter
public class BulkOrderStatusRequest {

    @NotEmpty(message = "Danh sách order không được để trống")
    private List<Long> orderIds;

    @NotNull(message = "Trạng thái mới không được để trống")
    private OrderStatus newStatus;
}
