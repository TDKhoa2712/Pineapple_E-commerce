package backend.pineapple_ecommerce.modules.order.dto.request;

import backend.pineapple_ecommerce.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private Long addressId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    private String note;

    private String couponCode;
}
