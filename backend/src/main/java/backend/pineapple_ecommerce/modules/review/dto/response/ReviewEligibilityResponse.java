package backend.pineapple_ecommerce.modules.review.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewEligibilityResponse {
    private boolean eligible;
    private long purchasedQuantity;
    private long reviewedCount;
}
