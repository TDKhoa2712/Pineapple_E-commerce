package backend.pineapple_ecommerce.modules.order.dto.request;

import backend.pineapple_ecommerce.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Body cho API PATCH /api/v1/orders/admin/{orderId}/status */
@Getter
@Setter
public class UpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private OrderStatus status;
}
