package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.CreateReviewRequest;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.ReviewResponse;
import backend.pineapple_ecommerce.entity.Review;
import backend.pineapple_ecommerce.entity.ReviewImage;
import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.mapper.ReviewMapper;
import backend.pineapple_ecommerce.repository.OrderRepository;
import backend.pineapple_ecommerce.repository.ProductRepository;
import backend.pineapple_ecommerce.repository.ReviewRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.ReviewService;
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

    private final ReviewRepository  reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;
    private final OrderRepository   orderRepository;
    private final ReviewMapper      reviewMapper;

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        // 1. Kiểm tra sản phẩm tồn tại
        var product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 2. Kiểm tra đã mua sản phẩm chưa (có đơn DELIVERED chứa sản phẩm này)
        boolean hasPurchased = orderRepository
                .findByUserId(userId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent()
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .anyMatch(item -> item.getProduct().getId().equals(request.getProductId()));

        if (!hasPurchased) {
            throw new BusinessException("Bạn chỉ có thể đánh giá sản phẩm đã mua và nhận hàng");
        }

        // 3. Kiểm tra đã review chưa (mỗi user chỉ review 1 lần)
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này rồi");
        }

        // 4. Tạo Review entity
        Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setProduct(product);

        // 5. Gắn ảnh review (nếu có)
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        Page<ReviewResponse> result = reviewRepository
                .findByProductId(productId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(reviewMapper::toResponse);
        return PageResponse.of(result);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        // Chỉ chủ review hoặc Admin mới được xoá
        // (kiểm tra Admin role ở tầng Controller với @PreAuthorize)
        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xoá đánh giá này");
        }

        reviewRepository.delete(review);
        log.info("Review {} deleted by userId={}", reviewId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        return reviewRepository.getAverageRatingByProductId(productId);
    }
}
