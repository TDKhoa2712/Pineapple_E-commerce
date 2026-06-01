package backend.pineapple_ecommerce.modules.review.service;

import backend.pineapple_ecommerce.modules.review.dto.request.CreateReviewRequest;
import backend.pineapple_ecommerce.modules.review.dto.request.UpdateReviewRequest;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.review.dto.response.ReviewResponse;
import backend.pineapple_ecommerce.modules.review.dto.response.ReviewRatingResponse;
import backend.pineapple_ecommerce.modules.review.dto.response.ReviewEligibilityResponse;

/**
 * Quản lý đánh giá sản phẩm.
 * Quy tắc nghiệp vụ: mỗi user chỉ được review 1 lần mỗi sản phẩm,
 * và chỉ khi đã mua sản phẩm đó (order ở trạng thái DELIVERED).
 */
public interface ReviewService {

    /** Tạo đánh giá. Kiểm tra: đã mua hàng, chưa review trước đó. */
    ReviewResponse createReview(Long userId, CreateReviewRequest request);

    /**
     * Cập nhật đánh giá — chỉ chủ review mới được sửa.
     * NEW — 2.2
     */
    ReviewResponse updateReview(Long reviewId, Long userId, UpdateReviewRequest request);

    /**
     * Lấy review public của sản phẩm — phân trang, loại bỏ isHidden.
     * NEW: thêm filter rating (nullable — null = tất cả sao).
     */
    PageResponse<ReviewResponse> getProductReviews(Long productId, Integer rating, int page, int size);

    /** Compat: gọi getProductReviews với rating=null */
    default PageResponse<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        return getProductReviews(productId, null, page, size);
    }

    /** Xoá review — chỉ chủ review hoặc Admin mới được xoá. */
    void deleteReview(Long reviewId, Long userId);

    /** Admin xoá review bất kỳ (kèm cleanup ảnh Cloudinary). */
    void adminDeleteReview(Long reviewId);

    /**
     * Admin ẩn review vi phạm mà không xoá.
     * isHidden = true → không hiển thị public nhưng Admin vẫn thấy.
     * NEW — 2.2
     */
    void hideReview(Long reviewId);

    /**
     * User vote review "hữu ích" hoặc "không hữu ích".
     * Nếu đã vote trước đó: cập nhật isHelpful.
     * Cập nhật denormalized helpfulCount / unhelpfulCount trên Review.
     * NEW — 2.2
     */
    void voteReview(Long reviewId, Long userId, Boolean helpful);

    /** Rating trung bình của sản phẩm (double 1.0 – 5.0). */
    Double getAverageRating(Long productId);

    /** Chi tiết rating của sản phẩm bao gồm số sao trung bình, tổng review và phân phối sao. */
    ReviewRatingResponse getProductRatingStats(Long productId);

    /** Admin: lấy tất cả review có filter keyword, rating, productId, userId và sort. */
    PageResponse<ReviewResponse> getAllReviews(int page, int size, String keyword, Integer rating, Long productId, Long userId, String sortBy, String sortDirection);

    /** Kiểm tra quyền đánh giá sản phẩm của user hiện tại. */
    ReviewEligibilityResponse checkReviewEligibility(Long userId, Long productId);
}
