package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.AddToCartRequest;
import backend.pineapple_ecommerce.dto.request.MergeCartRequest;
import backend.pineapple_ecommerce.dto.request.UpdateCartItemRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.CartResponse;
import backend.pineapple_ecommerce.dto.response.CartValidationResponse;
import backend.pineapple_ecommerce.dto.response.MergeCartResponse;
import backend.pineapple_ecommerce.service.CartService;
import backend.pineapple_ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Cart", description = "Quản lý giỏ hàng")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    // ─────────────────────────────────────────────
    // EXISTING
    // ─────────────────────────────────────────────

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
                cartService.updateCartItem(userId, cartItemId, request),
                "Cập nhật giỏ hàng thành công"));
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

    // ─────────────────────────────────────────────
    // NEW — 2.6
    // ─────────────────────────────────────────────

    /**
     * NEW: Số lượng items trong giỏ — dùng cho badge trên header FE.
     * Trả về { "itemCount": 3 }
     */
    @Operation(summary = "Số lượng items trong giỏ (badge header)")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getCartCount() {
        Long userId = userService.getCurrentUserId();
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("itemCount", count)));
    }

    /**
     * NEW: Validate giỏ hàng trước khi checkout.
     * FE gọi endpoint này trước khi redirect sang trang đặt hàng.
     * Trả về danh sách warning và estimatedTotal.
     */
    @Operation(summary = "Validate giỏ hàng trước khi checkout")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CartValidationResponse>> validateCart() {
        Long userId = userService.getCurrentUserId();
        CartValidationResponse result = cartService.validateCart(userId);
        String message = result.isValid()
                ? "Giỏ hàng hợp lệ, có thể tiến hành đặt hàng"
                : "Giỏ hàng có " + result.getWarnings().size() + " vấn đề cần xử lý";
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    /**
     * Gộp giỏ hàng khách (localStorage) vào giỏ hàng thật sau khi đăng nhập.
     *
     * FE gọi ngay sau khi nhận JWT token thành công:
     *   POST /api/v1/cart/merge
     *   Authorization: Bearer <new_jwt>
     *   Body: { "items": [{ "productId": 1, "quantity": 2 }] }
     *
     * Nếu localStorage rỗng → gửi body { "items": [] } → trả về giỏ hàng hiện tại không thay đổi.
     */
    @Operation(summary = "Gộp giỏ hàng khách (localStorage) vào giỏ hàng user sau đăng nhập")
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<MergeCartResponse>> mergeGuestCart(
            @Valid @RequestBody MergeCartRequest request) {
        Long userId = userService.getCurrentUserId();
        MergeCartResponse result = cartService.mergeGuestCart(userId, request);

        String message = result.getSkippedItems().isEmpty()
                ? String.format("Đã gộp %d sản phẩm vào giỏ hàng", result.getMergedCount())
                : String.format("Đã gộp %d sản phẩm, %d sản phẩm bị bỏ qua (hết hàng hoặc ngừng bán)",
                result.getMergedCount(), result.getSkippedItems().size());

        return ResponseEntity.ok(ApiResponse.success(result, message));
    }
}
