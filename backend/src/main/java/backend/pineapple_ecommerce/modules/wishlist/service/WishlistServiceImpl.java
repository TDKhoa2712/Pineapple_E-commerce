package backend.pineapple_ecommerce.modules.wishlist.service;

import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.wishlist.mapper.WishlistMapper;
import backend.pineapple_ecommerce.modules.wishlist.models.Wishlist;
import backend.pineapple_ecommerce.modules.wishlist.repository.WishlistRepository;
import backend.pineapple_ecommerce.modules.wishlist.dto.response.WishlistResponse;
import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository     userRepository;
    private final ProductRepository  productRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistResponse> getMyWishlist(Long userId, int page, int size) {
        var result = wishlistRepository
                .findByUserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(wishlistMapper::toResponse);
        return PageResponse.of(result);
    }

    @Override
    @Transactional
    public boolean toggleWishlist(Long userId, Long productId) {
        // Nếu đã có → xoá (toggle off)
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            wishlistRepository.deleteByUserIdAndProductId(userId, productId);
            log.debug("Removed from wishlist: userId={}, productId={}", userId, productId);
            return false;
        }

        // Chưa có → thêm (toggle on)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        Wishlist wishlist = Wishlist.builder().user(user).product(product).build();
        wishlistRepository.save(wishlist);
        log.debug("Added to wishlist: userId={}, productId={}", userId, productId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
}
