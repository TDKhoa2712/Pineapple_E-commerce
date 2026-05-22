package backend.pineapple_ecommerce.modules.cart.service;

import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.modules.cart.models.Cart;
import backend.pineapple_ecommerce.modules.cart.dto.request.AddToCartRequest;
import backend.pineapple_ecommerce.modules.cart.dto.request.MergeCartRequest;
import backend.pineapple_ecommerce.modules.cart.dto.request.UpdateCartItemRequest;
import backend.pineapple_ecommerce.modules.cart.dto.response.CartResponse;
import backend.pineapple_ecommerce.modules.cart.dto.response.CartValidationResponse;
import backend.pineapple_ecommerce.modules.cart.dto.response.MergeCartResponse;

/**
 * Quản lý giỏ hàng của người dùng.
 * Mỗi user có duy nhất 1 Cart; các CartItem được add/update/remove.
 */
public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(Long userId, Long cartItemId);

    void clearCart(Long userId);

    int getCartItemCount(Long userId);

    CartValidationResponse validateCart(Long userId);

    MergeCartResponse mergeGuestCart(Long userId, MergeCartRequest request);

    // ─────────────────────────────────────────────
    // Order-domain bridge
    // ─────────────────────────────────────────────

    /**
     * Lấy danh sách CartItem kèm Product để OrderService dùng khi tạo đơn hàng.
     *
     * <p>Tách biệt khỏi {@link #getCart} (trả DTO) vì OrderService cần entity
     * thực sự để truy cập Product.getEffectivePrice(), Product.getName()... mà không
     * phải inject thêm CartRepository hay ProductRepository vào OrderService.
     *
     * <p>Ném {@link BusinessException}
     * nếu giỏ hàng rỗng — tránh tạo đơn trống.
     *
     * @param userId user đang checkout
     * @return danh sách CartItem đã eager-load Product, không rỗng
     */
    Cart getCheckoutItems(Long userId);
}