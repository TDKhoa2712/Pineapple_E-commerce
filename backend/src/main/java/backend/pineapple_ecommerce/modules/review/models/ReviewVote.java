package backend.pineapple_ecommerce.modules.review.models;

import backend.pineapple_ecommerce.common.entity.BaseEntity;
import backend.pineapple_ecommerce.modules.user.models.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Lưu vote "hữu ích / không hữu ích" cho một review.
 * Unique constraint (review_id, user_id): mỗi user chỉ vote 1 lần cho mỗi review.
 * Khi vote lại sẽ update isHelpful thay vì insert mới.
 */
@Entity
@Table(
        name = "review_votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_votes_review_user",
                columnNames = {"review_id", "user_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class ReviewVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** true = hữu ích, false = không hữu ích */
    @Column(name = "is_helpful", nullable = false)
    private Boolean isHelpful;
}
