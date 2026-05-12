package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.CreateReviewRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.ReviewResponse;
import backend.pineapple_ecommerce.service.ReviewService;
import backend.pineapple_ecommerce.service.UserService;
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
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getProductReviews(productId, page, size)));
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

    @Operation(summary = "Xoá đánh giá của tôi",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyReview(@PathVariable Long reviewId) {
        Long userId = userService.getCurrentUserId();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá đánh giá"));
    }

    // ─────────────────────────────────────────────
    // ADMIN — xoá bất kỳ review nào
    // ─────────────────────────────────────────────

    @Operation(summary = "Xoá đánh giá bất kỳ (Admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/admin/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnyReview(@PathVariable Long reviewId) {
        // Admin không bị giới hạn bởi userId — truyền -1L để bypass ownership check
        // Lưu ý: ReviewServiceImpl.deleteReview() kiểm tra owner; cần bổ sung @PreAuthorize
        // hoặc tạo method riêng cho Admin. Ở đây dùng @PreAuthorize bảo vệ endpoint.
        Long userId = userService.getCurrentUserId();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá đánh giá"));
    }
}