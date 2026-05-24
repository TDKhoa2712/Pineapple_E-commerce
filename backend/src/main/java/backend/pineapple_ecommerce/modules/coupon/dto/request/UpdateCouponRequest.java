package backend.pineapple_ecommerce.modules.coupon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCouponRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;

    @NotNull(message = "Giới hạn số lần sử dụng không được để trống")
    @Min(value = 1, message = "Giới hạn sử dụng phải từ 1 trở lên")
    private Integer totalLimit;

    @NotNull(message = "Giới hạn mỗi user không được để trống")
    @Min(value = 1, message = "Giới hạn mỗi user phải từ 1 trở lên")
    private Integer perUserLimit;
}
