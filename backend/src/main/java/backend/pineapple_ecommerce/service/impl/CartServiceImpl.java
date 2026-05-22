package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.AddToCartRequest;
import backend.pineapple_ecommerce.dto.request.MergeCartRequest;
import backend.pineapple_ecommerce.dto.request.UpdateCartItemRequest;
import backend.pineapple_ecommerce.dto.response.CartResponse;
import backend.pineapple_ecommerce.dto.response.CartValidationResponse;
import backend.pineapple_ecommerce.dto.response.MergeCartResponse;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository           cartRepository;
    private final CartItemRepository       cartItemRepository;
    private final ProductRepository        productRepository;
    private final UserRepository           userRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final CartMapper               cartMapper;

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
        Cart cart = getOrCreateCart(userId);           // ← Lazy create
        Product product = findActiveProduct(request.getProductId());

        int availableStock = getAvailableStock(product.getId());
        if (availableStock <= 0) {
            throw new BusinessException("Sản phẩm đã hết hàng: " + product.getName());
        }

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
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = findCartItemAndVerifyOwner(cartItemId, userId);

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

    @Override
    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = findCartItemAndVerifyOwner(cartItemId, userId);
        cartItemRepository.delete(cartItem);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        log.info("Cart cleared for userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .map(cart -> cart.getItems().stream()
                        .mapToInt(CartItem::getQuantity)
                        .sum())
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public CartValidationResponse validateCart(Long userId) {
        Cart cart = getOrCreateCart(userId);   // ← Lazy create
        // ... (phần còn lại giữ nguyên)
        List<CartValidationResponse.CartItemWarning> warnings = new ArrayList<>();
        BigDecimal estimatedTotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            if (product.getStatus() != ProductStatus.ACTIVE) {
                warnings.add(CartValidationResponse.CartItemWarning.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .warningType("PRODUCT_INACTIVE")
                        .message("Sản phẩm không còn kinh doanh")
                        .requestedQty(item.getQuantity())
                        .availableQty(0)
                        .build());
                continue;
            }

            int available = getAvailableStock(product.getId());

            if (available <= 0) {
                warnings.add(CartValidationResponse.CartItemWarning.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .warningType("OUT_OF_STOCK")
                        .message("Sản phẩm đã hết hàng")
                        .requestedQty(item.getQuantity())
                        .availableQty(0)
                        .build());
            } else if (available < item.getQuantity()) {
                warnings.add(CartValidationResponse.CartItemWarning.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .warningType("INSUFFICIENT_STOCK")
                        .message("Chỉ còn " + available + " sản phẩm trong kho")
                        .requestedQty(item.getQuantity())
                        .availableQty(available)
                        .build());
                estimatedTotal = estimatedTotal.add(
                        product.getEffectivePrice().multiply(BigDecimal.valueOf(available)));
            } else {
                estimatedTotal = estimatedTotal.add(
                        product.getEffectivePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        return CartValidationResponse.builder()
                .isValid(warnings.isEmpty())
                .warnings(warnings)
                .estimatedTotal(estimatedTotal)
                .build();
    }

    @Override
    @Transactional
    public MergeCartResponse mergeGuestCart(Long userId, MergeCartRequest request) {
        Cart cart = getOrCreateCart(userId);   // ← Rất quan trọng
        // ... (phần logic merge giữ nguyên như cũ)
        int mergedCount = 0;
        List<MergeCartResponse.SkippedItem> skippedItems = new ArrayList<>();

        for (MergeCartRequest.CartItemMerge guestItem : request.getItems()) {
            // ... (giữ nguyên toàn bộ logic merge)
            Long productId = guestItem.getProductId();
            int requestedQty = guestItem.getQuantity();

            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                skippedItems.add(MergeCartResponse.SkippedItem.builder()
                        .productId(productId)
                        .productName("Không xác định")
                        .reason("PRODUCT_NOT_FOUND")
                        .message("Sản phẩm không tồn tại")
                        .requestedQty(requestedQty)
                        .actualQty(0)
                        .build());
                continue;
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                skippedItems.add(MergeCartResponse.SkippedItem.builder()
                        .productId(productId)
                        .productName(product.getName())
                        .reason("PRODUCT_INACTIVE")
                        .message("Sản phẩm không còn kinh doanh")
                        .requestedQty(requestedQty)
                        .actualQty(0)
                        .build());
                continue;
            }

            int availableStock = getAvailableStock(productId);
            if (availableStock <= 0) {
                skippedItems.add(MergeCartResponse.SkippedItem.builder()
                        .productId(productId)
                        .productName(product.getName())
                        .reason("OUT_OF_STOCK")
                        .message("Sản phẩm đã hết hàng")
                        .requestedQty(requestedQty)
                        .actualQty(0)
                        .build());
                continue;
            }

            CartItem existingItem = cartItemRepository
                    .findByCartIdAndProductId(cart.getId(), productId)
                    .orElse(null);

            int currentQty = existingItem != null ? existingItem.getQuantity() : 0;
            int desiredQty = currentQty + requestedQty;
            int finalQty = Math.min(desiredQty, availableStock);

            if (existingItem != null) {
                if (finalQty > currentQty) {
                    existingItem.setQuantity(finalQty);
                    cartItemRepository.save(existingItem);
                    mergedCount++;
                }
            } else if (finalQty > 0) {
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(finalQty)
                        .build();
                cartItemRepository.save(newItem);
                cart.getItems().add(newItem);
                mergedCount++;
            }
        }

        CartResponse cartResponse = cartMapper.toCartResponse(
                cartRepository.findByUserIdWithItems(userId).orElse(cart));

        return MergeCartResponse.builder()
                .cart(cartResponse)
                .mergedCount(mergedCount)
                .skippedItems(skippedItems)
                .build();
    }

    @Override
    public Cart getCheckoutItems(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────


    // ─────────────────────────────────────────────
    // PRIVATE HELPER - LAZY CART INITIALIZATION
    // ─────────────────────────────────────────────

    /**
     * Lazy Initialization: Chỉ tạo Cart khi user thực sự dùng giỏ hàng
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

    int getAvailableStock(Long productId) {
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
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền thao tác item này");
        }
        return cartItem;
    }
}