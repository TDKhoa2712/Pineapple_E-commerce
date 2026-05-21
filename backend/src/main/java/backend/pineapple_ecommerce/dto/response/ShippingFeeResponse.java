package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Phí giao hàng trả về cho client sau khi tính từ GHN.
 */
@Getter
@Builder
public class ShippingFeeResponse {

    /** Phí dịch vụ vận chuyển (VNĐ) */
    private BigDecimal serviceFee;

    /** Phí bảo hiểm hàng hóa (VNĐ) */
    private BigDecimal insuranceFee;

    /** Tổng phí phải trả (serviceFee + insuranceFee + phụ phí khác) */
    private BigDecimal totalFee;

    /** Phí COD (nếu thanh toán khi nhận hàng) */
    private BigDecimal codFee;

    /** Chiết khấu từ coupon (nếu có) */
    private BigDecimal couponDiscount;

    /** Thời gian giao dự kiến (ISO string) */
    private String expectedDeliveryTime;

    /** Service type đang dùng */
    private Integer serviceTypeId;

    /** Service ID cụ thể được chọn */
    private Integer serviceId;
}
