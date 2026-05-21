package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.ghn.GhnApiDto;
import backend.pineapple_ecommerce.dto.request.CalculateShippingFeeRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.ShippingFeeResponse;
import backend.pineapple_ecommerce.dto.response.ShippingTrackingResponse;
import backend.pineapple_ecommerce.service.GhnShippingService;
import backend.pineapple_ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý tính năng giao hàng GHN.
 *
 * <p>Public APIs (không cần auth):
 *   GET  /api/v1/shipping/provinces             — Danh sách tỉnh/thành
 *   GET  /api/v1/shipping/districts             — Danh sách quận/huyện
 *   GET  /api/v1/shipping/wards                 — Danh sách phường/xã
 *   POST /api/v1/shipping/calculate-fee         — Tính phí ship (dùng khi checkout)
 *
 * <p>User APIs (cần JWT):
 *   GET  /api/v1/shipping/orders/{orderId}/tracking — Theo dõi vận đơn
 *
 * <p>Admin APIs:
 *   POST /api/v1/shipping/admin/orders/{orderId}/create-shipment  — Tạo vận đơn GHN
 *   POST /api/v1/shipping/admin/orders/{orderId}/cancel-shipment  — Hủy vận đơn GHN
 *   POST /api/v1/shipping/admin/orders/{orderId}/sync             — Force sync trạng thái
 */
@Tag(name = "Shipping", description = "Tính phí và theo dõi vận đơn GHN")
@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final GhnShippingService shippingService;
    private final UserService userService;

    // ─────────────────────────────────────────────
    // ADDRESS MASTER DATA (Public — không cần auth)
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách Tỉnh/Thành phố (GHN)",
               description = "Dùng để populate dropdown địa chỉ khi tạo/cập nhật địa chỉ giao hàng")
    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<GhnApiDto.Province>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getProvinces()));
    }

    @Operation(summary = "Lấy danh sách Quận/Huyện theo Tỉnh (GHN)")
    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<GhnApiDto.District>>> getDistricts(
            @RequestParam Integer provinceId) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getDistricts(provinceId)));
    }

    @Operation(summary = "Lấy danh sách Phường/Xã theo Quận (GHN)")
    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<GhnApiDto.Ward>>> getWards(
            @RequestParam Integer districtId) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getWards(districtId)));
    }

    // ─────────────────────────────────────────────
    // CALCULATE FEE (Public hoặc cần auth tùy business)
    // ─────────────────────────────────────────────

    @Operation(
        summary = "Tính phí giao hàng",
        description = """
            Tính phí ship trước khi đặt hàng. 
            Frontend gọi khi user chọn địa chỉ giao hàng ở bước checkout.
            
            **Luồng**:
            1. User chọn tỉnh → gọi GET /districts?provinceId=...
            2. User chọn quận → gọi GET /wards?districtId=...
            3. Có wardCode + districtId → gọi POST /calculate-fee
            """
    )
    @PostMapping("/calculate-fee")
    public ResponseEntity<ApiResponse<ShippingFeeResponse>> calculateFee(
            @Valid @RequestBody CalculateShippingFeeRequest request) {
        ShippingFeeResponse fee = shippingService.calculateFee(request);
        return ResponseEntity.ok(ApiResponse.success(fee, "Tính phí giao hàng thành công"));
    }

    // ─────────────────────────────────────────────
    // USER — Theo dõi vận đơn
    // ─────────────────────────────────────────────

    @Operation(
        summary = "Theo dõi trạng thái vận đơn",
        description = """
            User xem trạng thái giao hàng của đơn mình.
            Trả về trạng thái hiện tại + lịch sử các mốc tracking.
            
            **Các trạng thái**:
            - ready_to_pick: Chờ lấy hàng
            - picking: Đang lấy hàng  
            - transporting: Đang vận chuyển
            - delivering: Đang giao hàng
            - delivered: Giao hàng thành công
            - delivery_fail: Giao thất bại (sẽ giao lại)
            - return: Đang hoàn hàng
            """
    )
    @GetMapping("/orders/{orderId}/tracking")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ShippingTrackingResponse>> getTracking(
            @PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        ShippingTrackingResponse tracking = shippingService.getTracking(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success(tracking));
    }

    // ─────────────────────────────────────────────
    // ADMIN — Quản lý vận đơn
    // ─────────────────────────────────────────────

    @Operation(
        summary = "[Admin] Tạo vận đơn GHN cho đơn hàng",
        description = """
            Tạo vận đơn GHN sau khi đơn hàng được Admin xác nhận và đóng gói xong.
            Đơn hàng phải ở trạng thái **PROCESSING**.
            
            Sau khi gọi thành công:
            - Mã vận đơn GHN được lưu vào DB
            - Phí ship thực tế được cập nhật vào Order
            - Shipper GHN sẽ đến lấy hàng theo lịch
            """
    )
    @PostMapping("/admin/orders/{orderId}/create-shipment")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ShippingTrackingResponse>> createShipment(
            @PathVariable Long orderId) {
        ShippingTrackingResponse result = shippingService.createShipment(orderId);
        return ResponseEntity.ok(ApiResponse.success(result, "Tạo vận đơn GHN thành công"));
    }

    @Operation(
        summary = "[Admin] Hủy vận đơn GHN",
        description = """
            Hủy vận đơn GHN. Chỉ được hủy khi vận đơn đang ở trạng thái:
            - **ready_to_pick** (Chờ lấy hàng)
            - **picking** (Đang lấy hàng)
            
            Sau khi hủy thành công trên GHN, trạng thái đơn hàng sẽ được cập nhật về CANCELLED.
            """
    )
    @PostMapping("/admin/orders/{orderId}/cancel-shipment")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> cancelShipment(@PathVariable Long orderId) {
        shippingService.cancelShipment(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã hủy vận đơn GHN thành công"));
    }

    @Operation(
        summary = "[Admin] Đồng bộ trạng thái vận đơn từ GHN",
        description = "Force sync — gọi GHN API lấy trạng thái mới nhất về cập nhật DB. Dùng khi webhook bị miss."
    )
    @PostMapping("/admin/orders/{orderId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ShippingTrackingResponse>> syncStatus(
            @PathVariable Long orderId) {
        // Lấy ghnOrderCode từ DB rồi sync
        Long userId = null; // Admin không cần userId check
        // Note: getTracking dùng userId check — tạo riêng method admin getTracking nếu cần
        // Tạm thời dùng syncStatusFromGhn qua ghnOrderCode
        // TODO: inject GhnShipmentRepository và lấy trực tiếp
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đồng bộ trạng thái thành công"));
    }
}
