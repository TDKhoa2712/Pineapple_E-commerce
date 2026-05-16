package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewResponse {
    private Long    id;
    private Long    userId;
    private String  userFullName;
    private String  userAvatar;
    private Long    productId;
    private Integer rating;
    private String  comment;
    private List<String> imageUrls;

    /** NEW: số vote "hữu ích" */
    private Integer helpfulCount;

    /** NEW: số vote "không hữu ích" */
    private Integer unhelpfulCount;

    /**
     * NEW: review bị ẩn hay không.
     * Public API: luôn false (đã filter ở query).
     * Admin API: trả thật để Admin biết review nào đang bị ẩn.
     */
    private Boolean isHidden;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
