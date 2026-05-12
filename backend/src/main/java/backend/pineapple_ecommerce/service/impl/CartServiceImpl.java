package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.AddToCartRequest;
import backend.pineapple_ecommerce.dto.request.UpdateCartItemRequest;
import backend.pineapple_ecommerce.dto.response.CartResponse;
import backend.pineapple_ecommerce.entity.Cart;
import backend.pineapple_ecommerce.entity.CartItem;
import backend.pineapple_ecommerce.entity.Product;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.ProductStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.mapper.CartMapper;
import backend.pineapple_ecommerce.repository.CartItemRepository;
import backend.pineapple_ecommerce.repository.CartRepository;
import backend.pineapple_ecommerce.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.repository.ProductRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository            cartRepository;
    private final CartItemRepository        cartItemRepository;
    private final ProductRepository         productRepository;
    private final UserRepository            userRepository;
    private final InventoryBatchRepository  inventoryBatchRepository;
    private final CartMapper                cartMapper;

    // ─────────────────────────────────────────────
    // GET CART
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toCartResponse(cart);
    }

    // ─────────────────────────────────────────────
    // ADD TO CART
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Cart cart    = getOrCreateCart(userId);
        Product product = findActiveProduct(request.getProductId());

        // Kiểm tra tồn kho
        int availableStock = getAvailableStock(product.getId());
        if (availableStock <= 0) {
            throw new BusinessException("Sản phẩm đã hết hàng: " + product.getName());
        }

        // Nếu item đã tồn tại trong cart → cộng dồn số lượng
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (cartItem != null) {
            int newQty = cartItem.getQuantity() + request.getQuantity();
            validateStockSufficient(product, newQty, availableStock);
            cartItem.setQuantity(newQty);
            cartItemRepository.save(cartItem);
        } else {
            validateStockSufficient(product, request.getQuantity(), availableStock);
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        log.debug("Added to cart: userId={}, productId={}, qty={}", userId, product.getId(), request.getQuantity());
        return cartMapper.toCartResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    // ─────────────────────────────────────────────
    // UPDATE CART ITEM
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = findCartItemAndVerifyOwner(cartItemId, userId);

        // quantity = 0 → xoá item
        if (request.getQuantity() == 0) {
            cartItem.getCart().getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            int availableStock = getAvailableStock(cartItem.getProduct().getId());
            validateStockSufficient(cartItem.getProduct(), request.getQuantity(), availableStock);
            cartItem.setQuantity(request.getQuantity());
            cartItemRepository.save(cartItem);
        }

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        return cartMapper.toCartResponse(cart);
    }

    // ─────────────────────────────────────────────
    // REMOVE ITEM
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = findCartItemAndVerifyOwner(cartItemId, userId);
        cartItemRepository.delete(cartItem);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        return cartMapper.toCartResponse(cart);
    }

    // ─────────────────────────────────────────────
    // CLEAR CART
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        log.info("Cart cleared for userId={}", userId);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    /**
     * Lấy cart của user, nếu chưa có thì tạo mới (lazy init).
     * Trường hợp bình thường Cart đã được tạo lúc register.
     */
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    private Product findActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException("Sản phẩm không còn hoạt động: " + product.getName());
        }
        return product;
    }

    private int getAvailableStock(Long productId) {
        Integer stock = inventoryBatchRepository.getTotalAvailableStock(productId);
        return stock != null ? stock : 0;
    }

    private void validateStockSufficient(Product product, int requestedQty, int available) {
        if (requestedQty > available) {
            throw new BusinessException(
                    String.format("Sản phẩm '%s' chỉ còn %d trong kho, bạn đang yêu cầu %d",
                            product.getName(), available, requestedQty));
        }
    }

    private CartItem findCartItemAndVerifyOwner(Long cartItemId, Long userId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));

        // Đảm bảo cart item thuộc về đúng user
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền thao tác item này");
        }
        return cartItem;
    }
}
