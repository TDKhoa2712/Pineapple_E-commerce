package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Request tính phí giao hàng GHN.
 * Gọi tới: POST /v2/shipping-order/fee
 *
 * <p>Luồng sử dụng:
 * 1. Frontend gọi GET /api/v1/shipping/address?province=... để lấy to_district_id, to_ward_code
 * 2. Frontend gọi POST /api/v1/shipping/calculate-fee với districtId + wardCode + weight
 * 3. Backend gọi GHN và trả phí về cho frontend hiển thị trước khi checkout
 */
@Getter
@Setter
public class CalculateShippingFeeRequest {

    /**
     * District ID của địa chỉ nhận hàng.
     * Lấy từ API: GET /api/v1/shipping/districts?provinceId=...
     */
    @NotNull(message = "District ID không được để trống")
    private Integer toDistrictId;

    /**
     * Ward code của địa chỉ nhận hàng.
     * Lấy từ API: GET /api/v1/shipping/wards?districtId=...
     */
    @NotBlank(message = "Ward code không được để trống")
    private String toWardCode;

    /**
     * Tổng khối lượng đơn hàng (gram).
     * Nếu không truyền, hệ thống tự tính từ sản phẩm trong giỏ hàng.
     */
    @Min(value = 1, message = "Khối lượng tối thiểu 1 gram")
    private Integer weight = 500; // Mặc định 500g nếu sản phẩm chưa có weight

    /** Chiều dài (cm) — optional */
    private Integer length = 20;

    /** Chiều rộng (cm) — optional */
    private Integer width = 20;

    /** Chiều cao (cm) — optional */
    private Integer height = 10;

    /**
     * Giá trị khai báo bảo hiểm (VNĐ).
     * GHN dùng để tính phí bảo hiểm và bồi thường nếu hàng bị mất/hư.
     * Tối đa 5,000,000 VNĐ.
     */
    private Integer insuranceValue = 0;

    /**
     * Service type ID.
     * 2 = E-commerce Delivery (mặc định)
     * 5 = Traditional Delivery
     */
    private Integer serviceTypeId = 2;

    /** Coupon code GHN (nếu có) */
    private String coupon;
}
