package backend.pineapple_ecommerce.modules.review.models;

import backend.pineapple_ecommerce.common.entity.BaseEntity;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.modules.product.models.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_product", columnList = "product_id"),
        @Index(name = "idx_reviews_user",    columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer rating;  // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    // ─────────────────────────────────────────────
    // NEW FIELDS — 2.2 ReviewService
    // ─────────────────────────────────────────────

    /**
     * Admin ẩn review vi phạm mà không cần xoá —
     * isHidden = true → không hiển thị cho public nhưng Admin vẫn thấy.
     */
    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    /**
     * Số lượng vote "hữu ích" — denormalized để query nhanh.
     * Cập nhật đồng bộ khi user vote trong ReviewServiceImpl.
     */
    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    /**
     * Số lượng vote "không hữu ích" — denormalized.
     */
    @Column(name = "unhelpful_count", nullable = false)
    @Builder.Default
    private Integer unhelpfulCount = 0;

    // ─────────────────────────────────────────────
    // Relationships
    // ─────────────────────────────────────────────

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewVote> votes = new ArrayList<>();
}
