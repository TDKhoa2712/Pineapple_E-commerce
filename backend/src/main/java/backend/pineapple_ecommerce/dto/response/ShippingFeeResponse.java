package backend.pineapple_ecommerce.dto.response;

import backend.pineapple_ecommerce.enums.CarrierCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Phí giao hàng trả về cho client — thêm thông tin carrier.
 */
@Getter
@Builder
public class ShippingFeeResponse {

    /** Carrier đã tính phí */
    private CarrierCode carrierCode;
    private String carrierName;

    private BigDecimal serviceFee;
    private BigDecimal insuranceFee;
    private BigDecimal totalFee;
    private BigDecimal codFee;
    private BigDecimal couponDiscount;

    private String expectedDeliveryTime;
    private String serviceId;
}