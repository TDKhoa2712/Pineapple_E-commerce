package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.modules.cart.mapper.CartMapper;
import backend.pineapple_ecommerce.modules.cart.models.Cart;
import backend.pineapple_ecommerce.modules.cart.models.CartItem;
import backend.pineapple_ecommerce.modules.cart.dto.request.AddToCartRequest;
import backend.pineapple_ecommerce.modules.cart.dto.request.UpdateCartItemRequest;
import backend.pineapple_ecommerce.modules.cart.dto.response.CartResponse;
import backend.pineapple_ecommerce.modules.cart.repository.CartItemRepository;
import backend.pineapple_ecommerce.modules.cart.repository.CartRepository;
import backend.pineapple_ecommerce.modules.cart.service.CartServiceImpl;
import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.enums.ProductStatus;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.modules.inventory.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl")
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository        productRepository;
    @Mock private UserRepository           userRepository;
    @Mock private InventoryBatchRepository inventoryBatchRepository;
    @Mock private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    // ── Fixtures ──────────────────────────────────────────────────────

    private static final Long USER_ID    = 1L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long CART_ID    = 100L;
    private static final Long ITEM_ID    = 200L;

    private User    user;
    private Product activeProduct;
    private Cart    cart;

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).email("user@example.com").build();

        activeProduct = Product.builder()
                .id(PRODUCT_ID)
                .name("Dứa mật vàng")
                .price(new BigDecimal("25000"))
                .status(ProductStatus.ACTIVE)
                .build();

        cart = Cart.builder()
                .id(CART_ID)
                .user(user)
                .items(new ArrayList<>())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // getCart
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCart()")
    class GetCart {

        @Test
        @DisplayName("cart đã tồn tại → trả về CartResponse tương ứng")
        void givenExistingCart_shouldReturnCartResponse() {
            CartResponse expected = CartResponse.builder().build();
            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(cartMapper.toCartResponse(cart)).thenReturn(expected);

            CartResponse result = cartService.getCart(USER_ID);

            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("cart chưa tồn tại → tạo mới và trả về CartResponse rỗng")
        void givenNoCart_shouldCreateNewCartAndReturn() {
            Cart newCart = Cart.builder().id(999L).user(user).items(new ArrayList<>()).build();
            CartResponse expected = CartResponse.builder().build();

            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
            when(cartMapper.toCartResponse(newCart)).thenReturn(expected);

            CartResponse result = cartService.getCart(USER_ID);

            assertThat(result).isSameAs(expected);
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("cart chưa tồn tại và user không tồn tại → ném ResourceNotFoundException")
        void givenNoCartAndNoUser_shouldThrow() {
            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getCart(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // addToCart
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addToCart()")
    class AddToCart {

        private AddToCartRequest buildRequest(int qty) {
            AddToCartRequest req = new AddToCartRequest();
            req.setProductId(PRODUCT_ID);
            req.setQuantity(qty);
            return req;
        }

        @Test
        @DisplayName("thêm sản phẩm mới vào giỏ → CartItem được lưu")
        void givenNewProduct_shouldSaveNewCartItem() {
            AddToCartRequest req = buildRequest(2);
            CartResponse expected = CartResponse.builder().build();

            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct));
            when(inventoryBatchRepository.getTotalAvailableStock(PRODUCT_ID)).thenReturn(10);
            when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty());
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(cartMapper.toCartResponse(cart)).thenReturn(expected);

            CartResponse result = cartService.addToCart(USER_ID, req);

            assertThat(result).isSameAs(expected);
            verify(cartItemRepository).save(any(CartItem.class));
        }

        @Test
        @DisplayName("sản phẩm đã có trong giỏ → cộng dồn số lượng")
        void givenExistingCartItem_shouldAccumulateQuantity() {
            AddToCartRequest req = buildRequest(3);

            CartItem existing = CartItem.builder()
                    .id(ITEM_ID)
                    .cart(cart)
                    .product(activeProduct)
                    .quantity(2)
                    .build();
            cart.getItems().add(existing);

            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct));
            when(inventoryBatchRepository.getTotalAvailableStock(PRODUCT_ID)).thenReturn(10);
            when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(existing));
            when(cartItemRepository.save(existing)).thenReturn(existing);
            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(cartMapper.toCartResponse(any())).thenReturn(CartResponse.builder().build());

            cartService.addToCart(USER_ID, req);

            assertThat(existing.getQuantity()).isEqualTo(5); // 2 + 3
        }

        @Test
        @DisplayName("sản phẩm hết hàng (stock = 0) → ném BusinessException")
        void givenOutOfStock_shouldThrowBusinessException() {
            AddToCartRequest req = buildRequest(1);

            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct));
            when(inventoryBatchRepository.getTotalAvailableStock(PRODUCT_ID)).thenReturn(0);

            assertThatThrownBy(() -> cartService.addToCart(USER_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("hết hàng");
        }

        @Test
        @DisplayName("số lượng yêu cầu vượt tồn kho → ném BusinessException")
        void givenQuantityExceedsStock_shouldThrowBusinessException() {
            AddToCartRequest req = buildRequest(99);

            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct));
            when(inventoryBatchRepository.getTotalAvailableStock(PRODUCT_ID)).thenReturn(5);
            when(cartItemRepository.findByCartIdAndProductId(CART_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(USER_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chỉ còn");
        }

        @Test
        @DisplayName("sản phẩm INACTIVE → ném BusinessException")
        void givenInactiveProduct_shouldThrowBusinessException() {
            AddToCartRequest req = buildRequest(1);
            activeProduct.setStatus(ProductStatus.INACTIVE);

            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct));

            assertThatThrownBy(() -> cartService.addToCart(USER_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("không còn hoạt động");
        }

        @Test
        @DisplayName("productId không tồn tại → ném ResourceNotFoundException")
        void givenUnknownProduct_shouldThrowResourceNotFoundException() {
            AddToCartRequest req = buildRequest(1);

            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(USER_ID, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // updateCartItem
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCartItem()")
    class UpdateCartItem {

        private CartItem buildOwnedCartItem() {
            return CartItem.builder()
                    .id(ITEM_ID)
                    .cart(cart)
                    .product(activeProduct)
                    .quantity(3)
                    .build();
        }

        @Test
        @DisplayName("quantity > 0 → cập nhật số lượng")
        void givenPositiveQuantity_shouldUpdateQuantity() {
            CartItem item = buildOwnedCartItem();
            UpdateCartItemRequest req = new UpdateCartItemRequest();
            req.setQuantity(5);

            when(cartItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(inventoryBatchRepository.getTotalAvailableStock(PRODUCT_ID)).thenReturn(10);
            when(cartItemRepository.save(item)).thenReturn(item);
            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(cartMapper.toCartResponse(cart)).thenReturn(CartResponse.builder().build());

            cartService.updateCartItem(USER_ID, ITEM_ID, req);

            assertThat(item.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(item);
        }

        @Test
        @DisplayName("quantity = 0 → xoá item khỏi giỏ")
        void givenZeroQuantity_shouldDeleteItem() {
            CartItem item = buildOwnedCartItem();
            cart.getItems().add(item);
            UpdateCartItemRequest req = new UpdateCartItemRequest();
            req.setQuantity(0);

            when(cartItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(cartMapper.toCartResponse(cart)).thenReturn(CartResponse.builder().build());

            cartService.updateCartItem(USER_ID, ITEM_ID, req);

            verify(cartItemRepository).delete(item);
        }

        @Test
        @DisplayName("quantity vượt tồn kho → ném BusinessException")
        void givenQuantityExceedsStock_shouldThrow() {
            CartItem item = buildOwnedCartItem();
            UpdateCartItemRequest req = new UpdateCartItemRequest();
            req.setQuantity(50);

            when(cartItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(inventoryBatchRepository.getTotalAvailableStock(PRODUCT_ID)).thenReturn(10);

            assertThatThrownBy(() -> cartService.updateCartItem(USER_ID, ITEM_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chỉ còn");
        }

        @Test
        @DisplayName("item thuộc user khác → ném BusinessException")
        void givenItemBelongingToOtherUser_shouldThrow() {
            User otherUser = User.builder().id(99L).build();
            Cart otherCart = Cart.builder().id(999L).user(otherUser).items(new ArrayList<>()).build();
            CartItem item = CartItem.builder()
                    .id(ITEM_ID)
                    .cart(otherCart)
                    .product(activeProduct)
                    .quantity(1)
                    .build();

            UpdateCartItemRequest req = new UpdateCartItemRequest();
            req.setQuantity(2);

            when(cartItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> cartService.updateCartItem(USER_ID, ITEM_ID, req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // removeCartItem
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeCartItem()")
    class RemoveCartItem {

        @Test
        @DisplayName("item tồn tại và thuộc user → xoá thành công")
        void givenOwnedItem_shouldDeleteAndReturnUpdatedCart() {
            CartItem item = CartItem.builder()
                    .id(ITEM_ID).cart(cart).product(activeProduct).quantity(1).build();
            CartResponse expected = CartResponse.builder().build();

            when(cartItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
            when(cartMapper.toCartResponse(cart)).thenReturn(expected);

            CartResponse result = cartService.removeCartItem(USER_ID, ITEM_ID);

            assertThat(result).isSameAs(expected);
            verify(cartItemRepository).delete(item);
        }

        @Test
        @DisplayName("item không tồn tại → ném ResourceNotFoundException")
        void givenMissingItem_shouldThrow() {
            when(cartItemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.removeCartItem(USER_ID, ITEM_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // clearCart
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("clearCart()")
    class ClearCart {

        @Test
        @DisplayName("xoá toàn bộ items trong giỏ")
        void givenExistingCart_shouldDeleteAllItems() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

            cartService.clearCart(USER_ID);

            verify(cartItemRepository).deleteByCartId(CART_ID);
        }

        @Test
        @DisplayName("cart không tồn tại → ném ResourceNotFoundException")
        void givenMissingCart_shouldThrow() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.clearCart(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}