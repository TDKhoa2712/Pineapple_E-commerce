package backend.pineapple_ecommerce.modules.coupon.controller;

import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.common.enums.CouponType;
import backend.pineapple_ecommerce.modules.coupon.dto.request.CreateCouponRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.request.UpdateCouponRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponUsageResponse;
import backend.pineapple_ecommerce.modules.coupon.service.CouponService;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Coupons", description = "Quản lý mã giảm giá dành cho Admin")
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;
    private final UserService   userService;

    @Operation(summary = "Tạo mã giảm giá mới (Admin)")
    @PostMapping({"", "/"})
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {
        Long adminId = userService.getCurrentUserId();
        CouponResponse response = couponService.createCoupon(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo mã giảm giá thành công"));
    }

    @Operation(summary = "Lấy danh sách mã giảm giá (Admin) — filter & sort")
    @GetMapping({"", "/"})
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getAllCoupons(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) CouponType type,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CouponResponse> response = couponService.getAllCoupons(active, expired, type, sortBy, sortDirection, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Cập nhật mã giảm giá (Admin)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCouponRequest request) {
        CouponResponse response = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật mã giảm giá thành công"));
    }

    @Operation(summary = "Xem lịch sử sử dụng mã giảm giá (Admin)")
    @GetMapping("/{id}/usage")
    public ResponseEntity<ApiResponse<List<CouponUsageResponse>>> getCouponUsageHistory(
            @PathVariable Long id) {
        List<CouponUsageResponse> response = couponService.getCouponUsageHistory(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
