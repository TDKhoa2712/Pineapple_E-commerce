package backend.pineapple_ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request tính phí giao hàng — provider-agnostic.
 *
 * <p>Thay đổi so với phiên bản cũ:
 * <ul>
 *   <li>Đổi {@code toDistrictId} từ Integer → String (một số carrier dùng string ID)
 *   <li>Thêm field {@code carrier} để chỉ định carrier (optional)
 *   <li>Đổi {@code serviceTypeId} → {@code serviceType} kiểu String (flexible cho mọi carrier)
 * </ul>
 */
@Getter
@Setter
public class CalculateShippingFeeRequest {

    /**
     * ID quận/huyện của địa chỉ nhận hàng.
     * GHN: số nguyên dạng string "1454" | GHTK: không dùng
     */
    @NotBlank(message = "District ID không được để trống")
    private String toDistrictId;

    /**
     * Mã phường/xã của địa chỉ nhận hàng.
     * GHN: "21307" | GHTK: không dùng
     */
    @NotBlank(message = "Ward code không được để trống")
    private String toWardCode;

    /** Tổng khối lượng (gram). Mặc định 500g. */
    @Min(value = 1, message = "Khối lượng tối thiểu 1 gram")
    private Integer weight = 500;

    private Integer length = 20;
    private Integer width  = 20;
    private Integer height = 10;

    /** Giá trị bảo hiểm (VNĐ) */
    private Integer insuranceValue = 0;

    /**
     * Loại dịch vụ của carrier.
     * GHN: "2" (E-commerce) | "5" (Traditional)
     * GHTK: "road" | "fly"
     * Null = dùng default của carrier.
     */
    private String serviceTypeId;

    /** Coupon code (nếu có) */
    private String coupon;
}