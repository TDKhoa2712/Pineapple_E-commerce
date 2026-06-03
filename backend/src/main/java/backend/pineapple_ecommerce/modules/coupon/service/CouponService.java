package backend.pineapple_ecommerce.modules.coupon.service;

import backend.pineapple_ecommerce.common.enums.CouponType;
import backend.pineapple_ecommerce.modules.cart.models.CartItem;
import backend.pineapple_ecommerce.modules.coupon.dto.request.CouponPreviewRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.request.CreateCouponRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.request.UpdateCouponRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponPreviewResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponUsageResponse;
import backend.pineapple_ecommerce.modules.order.models.Order;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    CouponPreviewResponse previewCoupon(Long userId, CouponPreviewRequest request);

    BigDecimal applyAndLock(String code, Long userId, List<CartItem> cartItems, BigDecimal subtotal);

    void saveCouponUsage(String code, Long userId, Order order, BigDecimal discountApplied);

    void releaseCouponUsage(Long orderId);

    CouponResponse createCoupon(Long adminId, CreateCouponRequest request);

    CouponResponse updateCoupon(Long id, UpdateCouponRequest request);

    backend.pineapple_ecommerce.common.dto.response.PageResponse<CouponResponse> getAllCoupons(Boolean active, Boolean expired, CouponType type, String sortBy, String sortDirection, int page, int size);

    List<CouponUsageResponse> getCouponUsageHistory(Long couponId);
}
