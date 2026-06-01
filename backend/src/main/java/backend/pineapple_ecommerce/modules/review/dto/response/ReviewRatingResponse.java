package backend.pineapple_ecommerce.modules.review.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class ReviewRatingResponse {
    private Double averageRating;
    private Long   totalReviews;
    private Map<Integer, Long> distribution;
}
