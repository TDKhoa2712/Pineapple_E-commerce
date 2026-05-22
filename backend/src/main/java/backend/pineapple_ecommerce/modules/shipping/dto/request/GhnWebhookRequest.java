package backend.pineapple_ecommerce.modules.shipping.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnWebhookRequest {
    @JsonProperty("OrderCode")
    private String orderCode;

    @JsonProperty("ClientOrderCode")
    private String clientOrderCode;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Type")
    private String type;
}