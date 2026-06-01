package backend.pineapple_ecommerce.modules.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateReviewRequest {

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating tối thiểu 1")
    @Max(value = 5, message = "Rating tối đa 5")
    private Integer rating;

    @Size(max = 1000, message = "Bình luận không quá 1000 ký tự")
    private String comment;

    private List<ImageDto> images;

    @Getter
    @Setter
    public static class ImageDto {
        private String url;
        private String publicId;
    }
}
