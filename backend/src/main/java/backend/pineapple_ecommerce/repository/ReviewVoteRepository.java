package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    /**
     * Tìm vote của user cho review — dùng để kiểm tra đã vote chưa
     * và để update nếu đổi ý (helpful → unhelpful hoặc ngược lại).
     */
    Optional<ReviewVote> findByReviewIdAndUserId(Long reviewId, Long userId);

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);
}
