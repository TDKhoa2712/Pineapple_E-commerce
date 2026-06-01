package backend.pineapple_ecommerce.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ExchangeRequest {

    @NotBlank(message = "Mã xác thực không được để trống")
    private String code;
}
