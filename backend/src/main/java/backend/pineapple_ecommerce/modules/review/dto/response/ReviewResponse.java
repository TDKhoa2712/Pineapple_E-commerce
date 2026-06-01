package backend.pineapple_ecommerce.modules.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * NEW: Trạng thái vote của user hiện tại đối với review này.
     * true: Hữu ích (Like), false: Không hữu ích (Dislike), null: Chưa vote / chưa đăng nhập
     */
    private Boolean userVote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
