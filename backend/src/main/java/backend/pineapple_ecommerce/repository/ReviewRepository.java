package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Lấy review public của sản phẩm — loại trừ review bị ẩn (isHidden = true).
     * NEW: thêm filter rating (nullable).
     */
    @Query("""
        SELECT r FROM Review r
        LEFT JOIN FETCH r.user
        WHERE r.product.id = :productId
        AND r.isHidden = false
        AND (:rating IS NULL OR r.rating = :rating)
        ORDER BY r.createdAt DESC
    """)
    Page<Review> findByProductIdAndRating(
            @Param("productId") Long productId,
            @Param("rating")    Integer rating,
            Pageable pageable);

    /** Giữ lại để tương thích — gọi findByProductIdAndRating với rating=null */
    @Query("""
        SELECT r FROM Review r
        LEFT JOIN FETCH r.user
        WHERE r.product.id = :productId
        AND r.isHidden = false
    """)
    Page<Review> findByProductId(@Param("productId") Long productId, Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    int countByProductId(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isHidden = false")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * Admin: lấy tất cả review (kể cả isHidden = true),
     * lọc theo keyword (tên user / nội dung) và rating.
     */
    @Query("""
        SELECT r FROM Review r
        LEFT JOIN FETCH r.user
        LEFT JOIN FETCH r.product
        WHERE (:rating IS NULL OR r.rating = :rating)
        AND (:keyword IS NULL OR :keyword = ''
            OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.comment)       LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Review> findAllForAdmin(
            @Param("keyword") String keyword,
            @Param("rating")  Integer rating,
            Pageable pageable);
}
