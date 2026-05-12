package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.AddToCartRequest;
import backend.pineapple_ecommerce.dto.request.UpdateCartItemRequest;
import backend.pineapple_ecommerce.dto.response.CartResponse;

/**
 * Quản lý giỏ hàng của người dùng.
 * Mỗi user có duy nhất 1 Cart; các CartItem được add/update/remove.
 */
public interface CartService {

    /** Lấy giỏ hàng hiện tại của user (tạo mới nếu chưa có). */
    CartResponse getCart(Long userId);

    /**
     * Thêm sản phẩm vào giỏ.
     * - Nếu sản phẩm đã tồn tại trong giỏ: cộng dồn số lượng.
     * - Kiểm tra tồn kho trước khi add.
     */
    CartResponse addToCart(Long userId, AddToCartRequest request);

    /**
     * Cập nhật số lượng một item trong giỏ.
     * - quantity = 0: xoá item khỏi giỏ.
     * - Kiểm tra tồn kho trước khi update.
     */
    CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request);

    /** Xoá một item cụ thể khỏi giỏ hàng. */
    CartResponse removeCartItem(Long userId, Long cartItemId);

    /** Xoá toàn bộ items trong giỏ — gọi sau khi đặt hàng thành công. */
    void clearCart(Long userId);
}
