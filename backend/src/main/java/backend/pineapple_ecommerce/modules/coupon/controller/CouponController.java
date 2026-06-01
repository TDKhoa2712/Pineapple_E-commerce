package backend.pineapple_ecommerce.modules.coupon.controller;

import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.request.CouponPreviewRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponPreviewResponse;
import backend.pineapple_ecommerce.modules.coupon.service.CouponService;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Coupons", description = "Quản lý mã giảm giá của User")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CouponController {

    private final CouponService couponService;
    private final UserService   userService;

    @Operation(summary = "Xem trước số tiền được giảm giá")
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<CouponPreviewResponse>> previewCoupon(
            @Valid @RequestBody CouponPreviewRequest request) {
        Long userId = userService.getCurrentUserId();
        CouponPreviewResponse response = couponService.previewCoupon(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Áp dụng mã giảm giá thành công"));
    }
}
