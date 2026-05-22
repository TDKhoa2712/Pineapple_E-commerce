package backend.pineapple_ecommerce.modules.address.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map; // Nhớ import java.util.Map

@Getter
@Builder
public class AddressResponse {
    private Long id;
    private String receiverName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String detail;
    private Boolean isDefault;

    // Thêm field này để ánh xạ Map từ Mapper và trả về JSON object cho Frontend
    private Map<String, Object> carrierMetadata;

    // Helper: full address string
    public String getFullAddress() {
        return detail + ", " + ward + ", " + district + ", " + province;
    }
}