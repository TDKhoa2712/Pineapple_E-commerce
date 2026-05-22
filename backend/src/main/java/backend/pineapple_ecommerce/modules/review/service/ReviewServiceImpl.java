package backend.pineapple_ecommerce.modules.review.service;

import backend.pineapple_ecommerce.modules.review.repository.ReviewRepository;
import backend.pineapple_ecommerce.modules.review.models.ReviewVote;
import backend.pineapple_ecommerce.modules.review.repository.ReviewVoteRepository;
import backend.pineapple_ecommerce.modules.review.mapper.ReviewMapper;
import backend.pineapple_ecommerce.modules.review.models.Review;
import backend.pineapple_ecommerce.modules.review.models.ReviewImage;
import backend.pineapple_ecommerce.modules.review.dto.request.CreateReviewRequest;
import backend.pineapple_ecommerce.modules.review.dto.request.UpdateReviewRequest;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.review.dto.response.ReviewResponse;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import backend.pineapple_ecommerce.modules.order.repository.OrderRepository;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.infrastructure.cloudinary.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ProductRepository    productRepository;
    private final UserRepository       userRepository;
    private final OrderRepository      orderRepository;
    private final ReviewMapper reviewMapper;
    private final CloudinaryService    cloudinaryService;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        var product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // FIX: dùng 1 query thay vì load toàn bộ đơn hàng vào memory
        boolean hasPurchased = orderRepository
                .existsByUserIdAndProductIdAndDelivered(userId, request.getProductId());

        if (!hasPurchased) {
            throw new BusinessException("Bạn chỉ có thể đánh giá sản phẩm đã mua và nhận hàng");
        }

        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này rồi");
        }

        Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setProduct(product);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<ReviewImage> images = new ArrayList<>();
            for (String url : request.getImageUrls()) {
                images.add(ReviewImage.builder().review(review).imageUrl(url).build());
            }
            review.setImages(images);
        }

        Review saved = reviewRepository.save(review);
        log.info("Review created: userId={}, productId={}, rating={}", userId, request.getProductId(), request.getRating());
        return reviewMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // UPDATE — NEW 2.2
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long userId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền sửa đánh giá này");
        }

        review.setRating(request.getRating());
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        // Cập nhật ảnh nếu có truyền lên
        if (request.getImageUrls() != null) {
            review.getImages().clear();
            for (String url : request.getImageUrls()) {
                review.getImages().add(
                    ReviewImage.builder().review(review).imageUrl(url).build()
                );
            }
        }

        Review saved = reviewRepository.save(review);
        log.info("Review updated: id={}, userId={}", reviewId, userId);
        return reviewMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // GET PRODUCT REVIEWS
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(Long productId, Integer rating, int page, int size) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        // NEW: dùng query filter rating (isHidden = false đã được filter trong query)
        Page<ReviewResponse> result = reviewRepository
                .findByProductIdAndRating(productId, rating,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(reviewMapper::toResponse);
        return PageResponse.of(result);
    }

    // ─────────────────────────────────────────────
    // HIDE — NEW 2.2
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void hideReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        review.setIsHidden(!review.getIsHidden()); // toggle: ẩn ↔ hiện
        reviewRepository.save(review);
        log.info("Review {} isHidden toggled to: {}", reviewId, review.getIsHidden());
    }

    // ─────────────────────────────────────────────
    // VOTE — NEW 2.2
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void voteReview(Long reviewId, Long userId, Boolean helpful) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Không cho tự vote review của mình
        if (review.getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không thể vote đánh giá của chính mình");
        }

        var existingVote = reviewVoteRepository.findByReviewIdAndUserId(reviewId, userId);

        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            Boolean oldHelpful = vote.getIsHelpful();

            if (oldHelpful.equals(helpful)) {
                // Vote giống cũ → bỏ vote (toggle off)
                reviewVoteRepository.delete(vote);
                if (helpful) {
                    review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
                } else {
                    review.setUnhelpfulCount(Math.max(0, review.getUnhelpfulCount() - 1));
                }
            } else {
                // Đổi vote
                vote.setIsHelpful(helpful);
                reviewVoteRepository.save(vote);
                if (helpful) {
                    review.setHelpfulCount(review.getHelpfulCount() + 1);
                    review.setUnhelpfulCount(Math.max(0, review.getUnhelpfulCount() - 1));
                } else {
                    review.setUnhelpfulCount(review.getUnhelpfulCount() + 1);
                    review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
                }
            }
        } else {
            // Vote mới
            ReviewVote vote = ReviewVote.builder()
                    .review(review)
                    .user(user)
                    .isHelpful(helpful)
                    .build();
            reviewVoteRepository.save(vote);
            if (helpful) {
                review.setHelpfulCount(review.getHelpfulCount() + 1);
            } else {
                review.setUnhelpfulCount(review.getUnhelpfulCount() + 1);
            }
        }

        reviewRepository.save(review);
        log.info("Vote recorded: reviewId={}, userId={}, helpful={}", reviewId, userId, helpful);
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xoá đánh giá này");
        }
        reviewRepository.delete(review);
        log.info("Review {} deleted by userId={}", reviewId, userId);
    }

    @Override
    @Transactional
    public void adminDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        List<String> publicIds = review.getImages().stream()
                .map(ReviewImage::getPublicId)
                .filter(pid -> pid != null && !pid.isBlank())
                .toList();
        if (!publicIds.isEmpty()) {
            cloudinaryService.deleteImages(publicIds);
        }

        reviewRepository.delete(review);
        log.info("Admin deleted review: id={}", reviewId);
    }

    // ─────────────────────────────────────────────
    // RATING & ALL REVIEWS
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        return reviewRepository.getAverageRatingByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAllReviews(int page, int size, String keyword, Integer rating) {
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<ReviewResponse> result = reviewRepository
                .findAllForAdmin(safeKeyword, rating,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(reviewMapper::toResponse);

        return PageResponse.of(result);
    }
}
