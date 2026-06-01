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
import backend.pineapple_ecommerce.modules.review.dto.response.ReviewRatingResponse;
import backend.pineapple_ecommerce.modules.review.dto.response.ReviewEligibilityResponse;
import java.util.Map;
import java.util.HashMap;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import backend.pineapple_ecommerce.security.CustomUserDetails;
import backend.pineapple_ecommerce.modules.user.models.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        long purchasedQty = orderRepository.countDeliveredQuantityByUserIdAndProductId(userId, request.getProductId());

        if (purchasedQty <= 0) {
            throw new BusinessException("Bạn chỉ có thể đánh giá sản phẩm đã mua và nhận hàng");
        }

        long reviewsCount = reviewRepository.countByUserIdAndProductId(userId, request.getProductId());
        if (reviewsCount >= purchasedQty) {
            throw new BusinessException("Bạn đã đánh giá hết số lượng sản phẩm này đã mua");
        }

        Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setProduct(product);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<ReviewImage> images = new ArrayList<>();
            for (var imgDto : request.getImages()) {
                images.add(ReviewImage.builder()
                        .review(review)
                        .imageUrl(imgDto.getUrl())
                        .publicId(imgDto.getPublicId())
                        .build());
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
        populateUserVotes(result);
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
    public ReviewRatingResponse getProductRatingStats(Long productId) {
        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        if (avgRating == null) {
            avgRating = 0.0;
        }

        long totalReviews = reviewRepository.countVisibleReviewsByProductId(productId);

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        List<Object[]> distData = reviewRepository.getRatingDistributionByProductId(productId);
        if (distData != null) {
            for (Object[] row : distData) {
                Integer rating = (Integer) row[0];
                Long count = (Long) row[1];
                if (rating >= 1 && rating <= 5) {
                    distribution.put(rating, count);
                }
            }
        }

        return ReviewRatingResponse.builder()
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .distribution(distribution)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAllReviews(int page, int size, String keyword, Integer rating, Long productId, Long userId, String sortBy, String sortDirection) {
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Sort sort = Sort.by("createdAt").descending();
        if (sortBy != null && !sortBy.isBlank()) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, sortBy);
        }

        Page<ReviewResponse> result = reviewRepository
                .findAllForAdmin(safeKeyword, rating, productId, userId,
                        PageRequest.of(page, size, sort))
                .map(reviewMapper::toResponse);
        populateUserVotes(result);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkReviewEligibility(Long userId, Long productId) {
        long purchasedQty = orderRepository.countDeliveredQuantityByUserIdAndProductId(userId, productId);
        long reviewsCount = reviewRepository.countByUserIdAndProductId(userId, productId);
        return ReviewEligibilityResponse.builder()
                .eligible(purchasedQty > 0 && reviewsCount < purchasedQty)
                .purchasedQuantity(purchasedQty)
                .reviewedCount(reviewsCount)
                .build();
    }

    private void populateUserVotes(Page<ReviewResponse> responsePage) {
        if (responsePage.isEmpty()) {
            return;
        }
        Long currentUserId = getCurrentUserIdOptional();
        if (currentUserId == null) {
            return;
        }

        List<Long> reviewIds = responsePage.stream()
                .map(ReviewResponse::getId)
                .toList();

        List<ReviewVote> votes = reviewVoteRepository.findByUserIdAndReviewIdIn(currentUserId, reviewIds);
        Map<Long, Boolean> voteMap = votes.stream()
                .collect(Collectors.toMap(
                        v -> v.getReview().getId(),
                        ReviewVote::getIsHelpful,
                        (v1, v2) -> v1
                ));

        responsePage.forEach(resp -> resp.setUserVote(voteMap.get(resp.getId())));
    }

    private Long getCurrentUserIdOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUserId();
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }
}
