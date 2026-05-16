package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.AddToCartRequest;
import backend.pineapple_ecommerce.dto.request.MergeCartRequest;
import backend.pineapple_ecommerce.dto.request.UpdateCartItemRequest;
import backend.pineapple_ecommerce.dto.response.CartResponse;
import backend.pineapple_ecommerce.dto.response.CartValidationResponse;
import backend.pineapple_ecommerce.dto.response.MergeCartResponse;

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

    // ─────────────────────────────────────────────
    // NEW — 2.6
    // ─────────────────────────────────────────────

    /**
     * Tổng số lượng items trong giỏ — dùng cho badge trên FE header.
     * Tính SUM(quantity) không phải số lượng loại sản phẩm.
     */
    int getCartItemCount(Long userId);

    /**
     * Validate toàn bộ cart trước khi checkout.
     * Kiểm tra từng item: tồn kho còn đủ không, sản phẩm còn active không.
     * Trả về danh sách warnings (nếu có) và estimatedTotal.
     */
    CartValidationResponse validateCart(Long userId);

    /**
     * Gộp giỏ hàng khách (từ localStorage) vào giỏ hàng thật sau khi đăng nhập.
     *
     * Chiến lược SMART MERGE:
     * - Nếu sản phẩm đã có → cộng dồn số lượng (không reset)
     * - Nếu tổng vượt tồn kho → cap lại bằng tồn kho khả dụng, ghi vào skipped với reason=STOCK_CAPPED
     * - Sản phẩm không còn active → bỏ qua, ghi reason=PRODUCT_INACTIVE
     * - Sản phẩm hết hàng → bỏ qua, ghi reason=OUT_OF_STOCK
     *
     * @param userId  user vừa đăng nhập
     * @param request danh sách items từ localStorage
     * @return giỏ hàng đã merge + danh sách items bị bỏ qua
     */
    MergeCartResponse mergeGuestCart(Long userId, MergeCartRequest request);
}
