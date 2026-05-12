package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.AddToCartRequest;
import backend.pineapple_ecommerce.dto.request.UpdateCartItemRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.CartResponse;
import backend.pineapple_ecommerce.service.CartService;
import backend.pineapple_ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart", description = "Quản lý giỏ hàng")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @Operation(summary = "Lấy giỏ hàng hiện tại")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(userId)));
    }

    @Operation(summary = "Thêm sản phẩm vào giỏ")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {

        Long userId = userService.getCurrentUserId();
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã thêm vào giỏ hàng"));
    }

    @Operation(summary = "Cập nhật số lượng item trong giỏ (quantity=0 để xoá)")
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateCartItem(userId, cartItemId, request), "Cập nhật giỏ hàng thành công"));
    }

    @Operation(summary = "Xoá một item khỏi giỏ hàng")
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Long cartItemId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                cartService.removeCartItem(userId, cartItemId), "Đã xoá khỏi giỏ hàng"));
    }

    @Operation(summary = "Xoá toàn bộ giỏ hàng")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        Long userId = userService.getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá toàn bộ giỏ hàng"));
    }
}