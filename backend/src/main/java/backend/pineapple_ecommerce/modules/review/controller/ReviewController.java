package backend.pineapple_ecommerce.modules.review.controller;

import backend.pineapple_ecommerce.modules.review.service.ReviewService;
import backend.pineapple_ecommerce.modules.review.dto.request.CreateReviewRequest;
import backend.pineapple_ecommerce.modules.review.dto.request.UpdateReviewRequest;
import backend.pineapple_ecommerce.modules.review.dto.request.VoteReviewRequest;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.review.dto.response.ReviewResponse;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reviews", description = "Đánh giá sản phẩm")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService   userService;

    // ─────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách đánh giá của sản phẩm (public)")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer rating,       // NEW: filter theo sao
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getProductReviews(productId, rating, page, size)));
    }

    @Operation(summary = "Rating trung bình của sản phẩm (public)")
    @GetMapping("/product/{productId}/rating")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAverageRating(productId)));
    }

    // ─────────────────────────────────────────────
    // AUTHENTICATED USER
    // ─────────────────────────────────────────────

    @Operation(summary = "Tạo đánh giá (cần đã mua & nhận hàng)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request) {

        Long userId = userService.getCurrentUserId();
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đánh giá thành công"));
    }

    /**
     * NEW — 2.2: User sửa đánh giá của mình.
     */
    @Operation(summary = "Cập nhật đánh giá của tôi",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {

        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.updateReview(reviewId, userId, request), "Cập nhật đánh giá thành công"));
    }

    /**
     * NEW — 2.2: Vote "hữu ích" / "không hữu ích" cho review.
     * Vote lại cùng loại = bỏ vote; vote khác loại = đổi vote.
     */
    @Operation(summary = "Vote review hữu ích / không hữu ích",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{reviewId}/vote")
    public ResponseEntity<ApiResponse<Void>> voteReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody VoteReviewRequest request) {

        Long userId = userService.getCurrentUserId();
        reviewService.voteReview(reviewId, userId, request.getHelpful());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã ghi nhận vote của bạn"));
    }

    @Operation(summary = "Xoá đánh giá của tôi",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyReview(@PathVariable Long reviewId) {
        Long userId = userService.getCurrentUserId();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá đánh giá"));
    }

    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy tất cả đánh giá (Admin) — filter keyword + rating",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating) {

        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getAllReviews(page, size, keyword, rating)));
    }

    /**
     * NEW — 2.2: Admin ẩn/hiện review vi phạm mà không xoá.
     * Toggle: gọi lại endpoint sẽ hiện lại review.
     */
    @Operation(summary = "Ẩn/Hiện review vi phạm (Admin — toggle)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/admin/{reviewId}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleHideReview(@PathVariable Long reviewId) {
        reviewService.hideReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật trạng thái ẩn/hiện review"));
    }

    @Operation(summary = "Xoá đánh giá bất kỳ (Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/admin/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnyReview(@PathVariable Long reviewId) {
        reviewService.adminDeleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá đánh giá"));
    }
}
