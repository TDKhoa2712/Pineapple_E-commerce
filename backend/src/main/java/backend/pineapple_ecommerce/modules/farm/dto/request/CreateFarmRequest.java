package backend.pineapple_ecommerce.modules.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFarmRequest {

    @NotBlank(message = "Tên trang trại không được để trống")
    @Size(max = 200)
    private String name;

    @Size(max = 300)
    private String location;

    private String description;

    @Size(max = 500)
    private String certificate;

    private String imageUrl;
}
