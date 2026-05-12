package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

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

    // Helper: full address string
    public String getFullAddress() {
        return detail + ", " + ward + ", " + district + ", " + province;
    }
}
