package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.CreateReviewRequest;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.ReviewResponse;

/**
 * Quản lý đánh giá sản phẩm.
 * Quy tắc nghiệp vụ: mỗi user chỉ được review 1 lần mỗi sản phẩm,
 * và chỉ khi đã mua sản phẩm đó (order ở trạng thái DELIVERED).
 */
public interface ReviewService {

    /**
     * Tạo đánh giá.
     * Kiểm tra: đã mua hàng, chưa review trước đó.
     */
    ReviewResponse createReview(Long userId, CreateReviewRequest request);

    /** Lấy tất cả review của một sản phẩm — phân trang, sắp xếp mới nhất trước. */
    PageResponse<ReviewResponse> getProductReviews(Long productId, int page, int size);

    /** Xoá review — chỉ chủ review hoặc Admin mới được xoá. */
    void deleteReview(Long reviewId, Long userId);

    /** Rating trung bình của sản phẩm (double 1.0 – 5.0). */
    Double getAverageRating(Long productId);
}
