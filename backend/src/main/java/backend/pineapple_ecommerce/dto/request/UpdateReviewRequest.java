package backend.pineapple_ecommerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Body cho API PUT /api/v1/reviews/{id} — User sửa review của mình */
@Getter
@Setter
public class UpdateReviewRequest {

    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating tối thiểu là 1")
    @Max(value = 5, message = "Rating tối đa là 5")
    private Integer rating;

    private String comment;

    /** URL ảnh mới (đã upload lên Cloudinary từ trước) */
    private List<String> imageUrls;
}
