package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.WishlistResponse;

/**
 * Quản lý danh sách yêu thích.
 * Mỗi (user, product) là duy nhất — toggle add/remove.
 */
public interface WishlistService {

    /** Lấy danh sách yêu thích của user — phân trang. */
    PageResponse<WishlistResponse> getMyWishlist(Long userId, int page, int size);

    /**
     * Toggle: thêm nếu chưa có, xoá nếu đã tồn tại.
     *
     * @return true nếu vừa thêm vào, false nếu vừa xoá ra
     */
    boolean toggleWishlist(Long userId, Long productId);

    /** Kiểm tra sản phẩm có trong wishlist của user không. */
    boolean isInWishlist(Long userId, Long productId);
}
