package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Body cho API POST /api/v1/reviews/{id}/vote */
@Getter
@Setter
public class VoteReviewRequest {

    @NotNull(message = "Trường helpful không được để trống")
    private Boolean helpful;
}
