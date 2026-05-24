package backend.pineapple_ecommerce.modules.coupon.service;

import backend.pineapple_ecommerce.common.enums.CouponType;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.modules.category.models.Category;
import backend.pineapple_ecommerce.modules.cart.models.Cart;
import backend.pineapple_ecommerce.modules.cart.models.CartItem;
import backend.pineapple_ecommerce.modules.cart.service.CartService;
import backend.pineapple_ecommerce.modules.coupon.dto.request.CouponPreviewRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.request.CreateCouponRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.request.UpdateCouponRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponPreviewResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponUsageResponse;
import backend.pineapple_ecommerce.modules.coupon.mapper.CouponMapper;
import backend.pineapple_ecommerce.modules.coupon.models.Coupon;
import backend.pineapple_ecommerce.modules.coupon.models.CouponUsage;
import backend.pineapple_ecommerce.modules.coupon.repository.CouponRepository;
import backend.pineapple_ecommerce.modules.coupon.repository.CouponUsageRepository;
import backend.pineapple_ecommerce.modules.order.models.Order;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.modules.coupon.specification.CouponSpecification;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository      couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CartService           cartService;
    private final UserRepository        userRepository;
    private final CouponMapper          couponMapper;

    @Override
    @Transactional(readOnly = true)
    public CouponPreviewResponse previewCoupon(Long userId, CouponPreviewRequest request) {
        String code = request.getCouponCode().trim();
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        Cart cart = cartService.getCheckoutItems(userId);
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng trống, không thể áp dụng mã giảm giá");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal price = item.getProduct().getEffectivePrice();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Validate the coupon rules
        validateCoupon(coupon, userId, cart.getItems(), subtotal);

        // Compute discount using Enum strategy
        BigDecimal discount = coupon.getType().calculateDiscount(coupon, subtotal);
        BigDecimal newTotal = subtotal.subtract(discount).max(BigDecimal.ZERO);

        return CouponPreviewResponse.builder()
                .couponCode(coupon.getCode())
                .discountAmount(discount)
                .newTotal(newTotal)
                .build();
    }

    @Override
    @Transactional
    public BigDecimal applyAndLock(String code, Long userId, List<CartItem> cartItems, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        // Run validations
        validateCoupon(coupon, userId, cartItems, subtotal);

        // Optimistic locking / atomic update
        int updated = couponRepository.incrementUsedCount(coupon.getId());
        if (updated == 0) {
            throw new BusinessException("Mã giảm giá đã hết lượt sử dụng ngay lúc này");
        }

        // Calculate discount
        return coupon.getType().calculateDiscount(coupon, subtotal);
    }

    @Override
    @Transactional
    public void saveCouponUsage(String code, Long userId, Order order, BigDecimal discountApplied) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .order(order)
                .discountApplied(discountApplied)
                .usedAt(LocalDateTime.now())
                .build();

        couponUsageRepository.save(usage);
        log.info("Saved CouponUsage: couponId={}, userId={}, orderId={}, discount={}",
                coupon.getId(), userId, order.getId(), discountApplied);
    }

    @Override
    @Transactional
    public void releaseCouponUsage(Long orderId) {
        List<CouponUsage> usages = couponUsageRepository.findAllByOrderId(orderId);
        for (CouponUsage usage : usages) {
            couponRepository.decrementUsedCount(usage.getCoupon().getId());
            couponUsageRepository.delete(usage);
            log.info("Released CouponUsage: couponId={}, orderId={}",
                    usage.getCoupon().getId(), orderId);
        }
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(Long adminId, CreateCouponRequest request) {
        if (couponRepository.findByCodeIgnoreCase(request.getCode().trim()).isPresent()) {
            throw new BusinessException("Mã giảm giá đã tồn tại: " + request.getCode());
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (request.getStartDate().isAfter(request.getExpiryDate())) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày hết hạn");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim().toUpperCase())
                .type(request.getType())
                .value(request.getValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderValue(request.getMinOrderValue())
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .totalLimit(request.getTotalLimit())
                .perUserLimit(request.getPerUserLimit())
                .applicableProductIds(request.getApplicableProductIds() != null ? request.getApplicableProductIds() : new HashSet<>())
                .applicableCategoryIds(request.getApplicableCategoryIds() != null ? request.getApplicableCategoryIds() : new HashSet<>())
                .createdBy(admin)
                .usedCount(0)
                .isActive(true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        log.info("Created coupon: id={}, code={}", saved.getId(), saved.getCode());
        return couponMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long id, UpdateCouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id));

        coupon.setIsActive(request.getIsActive());
        coupon.setTotalLimit(request.getTotalLimit());
        coupon.setPerUserLimit(request.getPerUserLimit());

        Coupon saved = couponRepository.save(coupon);
        log.info("Updated coupon: id={}, active={}, totalLimit={}", id, saved.getIsActive(), saved.getTotalLimit());
        return couponMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons(Boolean active, Boolean expired, CouponType type) {
        Specification<Coupon> spec = Specification.allOf(
                CouponSpecification.isActive(active),
                CouponSpecification.isExpired(expired),
                CouponSpecification.hasType(type)
        );

        List<Coupon> coupons = couponRepository.findAll(spec);
        return coupons.stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponUsageResponse> getCouponUsageHistory(Long couponId) {
        // Fetch all usages for a coupon code
        List<CouponUsage> usages = couponUsageRepository.findAllByCouponId(couponId);

        return couponMapper.toUsageResponseList(usages);
    }

    private void validateCoupon(Coupon coupon, Long userId, List<CartItem> cartItems, BigDecimal subtotal) {
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new BusinessException("Mã giảm giá không còn hiệu lực");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getExpiryDate())) {
            throw new BusinessException("Mã giảm giá đã hết hạn hoặc chưa có hiệu lực");
        }

        if (coupon.getUsedCount() >= coupon.getTotalLimit()) {
            throw new BusinessException("Mã giảm giá đã hết lượt sử dụng");
        }

        // Check per user limit
        long usageCount = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
        if (usageCount >= coupon.getPerUserLimit()) {
            throw new BusinessException("Bạn đã sử dụng mã giảm giá này tối đa số lần cho phép");
        }

        // Check min order value
        if (subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BusinessException(String.format("Đơn hàng chưa đạt giá trị tối thiểu %sđ để sử dụng mã này",
                    coupon.getMinOrderValue().toPlainString()));
        }

        // Check product restrictions
        if (coupon.getApplicableProductIds() != null && !coupon.getApplicableProductIds().isEmpty()) {
            boolean hasApplicableProduct = cartItems.stream()
                    .anyMatch(item -> coupon.getApplicableProductIds().contains(item.getProduct().getId()));
            if (!hasApplicableProduct) {
                throw new BusinessException("Mã giảm giá không áp dụng cho các sản phẩm trong giỏ hàng");
            }
        }

        // Check category restrictions
        if (coupon.getApplicableCategoryIds() != null && !coupon.getApplicableCategoryIds().isEmpty()) {
            boolean hasApplicableCategory = cartItems.stream()
                    .anyMatch(item -> {
                        Category category = item.getProduct().getCategory();
                        return category != null && coupon.getApplicableCategoryIds().contains(category.getId());
                    });
            if (!hasApplicableCategory) {
                throw new BusinessException("Mã giảm giá không áp dụng cho các danh mục sản phẩm trong giỏ hàng");
            }
        }
    }
}
