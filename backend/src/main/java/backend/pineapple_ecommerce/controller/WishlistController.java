package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.WishlistResponse;
import backend.pineapple_ecommerce.service.UserService;
import backend.pineapple_ecommerce.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wishlist", description = "Danh sách yêu thích")
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserService     userService;

    @Operation(summary = "Lấy danh sách yêu thích của tôi")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WishlistResponse>>> getMyWishlist(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getMyWishlist(userId, page, size)));
    }

    @Operation(summary = "Toggle yêu thích sản phẩm (thêm nếu chưa có, xoá nếu đã có)")
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> toggle(@PathVariable Long productId) {
        Long userId = userService.getCurrentUserId();
        boolean added = wishlistService.toggleWishlist(userId, productId);
        String msg = added ? "Đã thêm vào yêu thích" : "Đã xoá khỏi yêu thích";
        return ResponseEntity.ok(ApiResponse.success(added, msg));
    }

    @Operation(summary = "Kiểm tra sản phẩm có trong danh sách yêu thích không")
    @GetMapping("/{productId}/check")
    public ResponseEntity<ApiResponse<Boolean>> check(@PathVariable Long productId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(wishlistService.isInWishlist(userId, productId)));
    }
}