package backend.pineapple_ecommerce.modules.address.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Request tạo/cập nhật địa chỉ giao hàng.
 *
 * <p>Thay đổi so với phiên bản cũ:
 * <ul>
 *   <li>Bỏ {@code ghnDistrictId}, {@code ghnWardCode}
 *   <li>Thêm {@code carrierMetadata} (JSON string) — lưu ID địa chỉ theo từng carrier
 * </ul>
 *
 * <p>Frontend gửi {@code carrierMetadata} khi user chọn địa chỉ từ dropdown carrier:
 * <pre>
 * {
 *   "receiverName": "Nguyen Van A",
 *   "phone": "0901234567",
 *   "province": "Hồ Chí Minh",
 *   "district": "Quận 1",
 *   "ward": "Phường Bến Nghé",
 *   "detail": "123 Lê Lợi",
 *   "carrierMetadata": "{\"GHN\":{\"districtId\":\"1442\",\"wardCode\":\"20308\"}}"
 * }
 * </pre>
 */
@Getter
@Setter
public class CreateAddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Tỉnh/Thành không được để trống")
    private String province;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String detail;

    private Boolean isDefault = false;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> carrierMetadata;
}