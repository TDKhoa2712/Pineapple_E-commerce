package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.CalculateShippingFeeRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.ShippingFeeResponse;
import backend.pineapple_ecommerce.dto.response.ShippingTrackingResponse;
import backend.pineapple_ecommerce.enums.CarrierCode;
import backend.pineapple_ecommerce.service.ShippingService;
import backend.pineapple_ecommerce.service.UserService;
import backend.pineapple_ecommerce.service.carrier.ShippingCarrierClient;
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
 * Controller quản lý tính năng giao hàng — hỗ trợ nhiều đơn vị vận chuyển.
 *
 * <p>Thay đổi so với phiên bản cũ:
 * <ul>
 *   <li>Inject {@link ShippingService} thay vì {@code GhnShippingService}
 *   <li>Thêm param {@code carrier} (optional) để chọn carrier
 *   <li>Thêm endpoint GET /carriers để list carriers đang hỗ trợ
 *   <li>Response bao gồm {@code carrierCode} và {@code carrierName}
 * </ul>
 */
@Tag(name = "Shipping", description = "Tính phí và theo dõi vận đơn (đa carrier)")
@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;
    private final UserService     userService;

    // ─────────────────────────────────────────────
    // Carriers
    // ─────────────────────────────────────────────

    @Operation(summary = "Danh sách đơn vị vận chuyển đang được hỗ trợ")
    @GetMapping("/carriers")
    public ResponseEntity<ApiResponse<List<CarrierCode>>> getSupportedCarriers() {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getSupportedCarriers()));
    }

    // ─────────────────────────────────────────────
    // Address Master Data
    // ─────────────────────────────────────────────

    @Operation(summary = "Danh sách Tỉnh/Thành phố",
            description = "Param `carrier` mặc định = carrier được cấu hình trong app (hiện tại: GHN)")
    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ShippingCarrierClient.LocationItem>>> getProvinces(
            @RequestParam(required = false) CarrierCode carrier) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getProvinces(carrier)));
    }

    @Operation(summary = "Danh sách Quận/Huyện theo Tỉnh")
    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<ShippingCarrierClient.LocationItem>>> getDistricts(
            @RequestParam String provinceId,
            @RequestParam(required = false) CarrierCode carrier) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getDistricts(carrier, provinceId)));
    }

    @Operation(summary = "Danh sách Phường/Xã theo Quận")
    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<ShippingCarrierClient.LocationItem>>> getWards(
            @RequestParam String districtId,
            @RequestParam(required = false) CarrierCode carrier) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getWards(carrier, districtId)));
    }

    // ─────────────────────────────────────────────
    // Calculate Fee
    // ─────────────────────────────────────────────

    @Operation(
            summary = "Tính phí giao hàng",
            description = """
            Tính phí ship trước khi đặt hàng.
            
            **Param `carrier`** (optional): Chọn carrier muốn tính phí.
            Nếu không truyền, dùng carrier mặc định (cấu hình trong app.shipping.default-carrier).
            
            **Luồng checkout**:
            1. GET /provinces → GET /districts → GET /wards
            2. POST /calculate-fee (có thể gọi nhiều lần với carrier khác nhau để so sánh)
            3. User chọn carrier và tiến hành đặt hàng
            """
    )
    @PostMapping("/calculate-fee")
    public ResponseEntity<ApiResponse<ShippingFeeResponse>> calculateFee(
            @Valid @RequestBody CalculateShippingFeeRequest request,
            @RequestParam(required = false) CarrierCode carrier) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.calculateFee(request, carrier),
                "Tính phí giao hàng thành công"
        ));
    }

    // ─────────────────────────────────────────────
    // USER — Tracking
    // ─────────────────────────────────────────────

    @Operation(
            summary = "Theo dõi trạng thái vận đơn",
            description = """
            User xem trạng thái giao hàng của đơn mình.
            Response bao gồm `carrierCode`, `carrierName` và `externalOrderCode`
            (mã vận đơn của carrier để user tra cứu độc lập trên app/web của carrier).
            """
    )
    @GetMapping("/orders/{orderId}/tracking")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ShippingTrackingResponse>> getTracking(
            @PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(shippingService.getTracking(orderId, userId)));
    }

    // ─────────────────────────────────────────────
    // ADMIN — Quản lý vận đơn
    // ─────────────────────────────────────────────

    @Operation(
            summary = "[Admin] Tạo vận đơn cho đơn hàng",
            description = """
            Tạo vận đơn sau khi đơn hàng được xác nhận và đóng gói xong (PROCESSING).
            
            **Param `carrier`** (optional): Chọn carrier xử lý đơn.
            Nếu không truyền → dùng carrier mặc định.
            """
    )
    @PostMapping("/admin/orders/{orderId}/create-shipment")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ShippingTrackingResponse>> createShipment(
            @PathVariable Long orderId,
            @RequestParam(required = false) CarrierCode carrier) {
        return ResponseEntity.ok(ApiResponse.success(
                shippingService.createShipment(orderId, carrier),
                "Tạo vận đơn thành công"
        ));
    }

    @Operation(
            summary = "[Admin] Hủy vận đơn",
            description = "Chỉ được hủy khi vận đơn đang 'Chờ lấy hàng' hoặc 'Đang lấy hàng'."
    )
    @PostMapping("/admin/orders/{orderId}/cancel-shipment")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> cancelShipment(@PathVariable Long orderId) {
        shippingService.cancelShipment(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã hủy vận đơn thành công"));
    }

    @Operation(
            summary = "[Admin] Force sync trạng thái vận đơn",
            description = "Gọi carrier API lấy trạng thái mới nhất. Dùng khi webhook bị miss."
    )
    @PostMapping("/admin/orders/{orderId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ShippingTrackingResponse>> syncStatus(
            @PathVariable Long orderId) {

        // Lấy carrier từ shipment hiện tại trong DB, rồi gọi sync
        ShippingTrackingResponse tracking = shippingService.getTracking(orderId, null);
        shippingService.syncStatus(
                tracking.getExternalOrderCode(),
                tracking.getCarrierCode()
        );

        return ResponseEntity.ok(ApiResponse.success(
                shippingService.getTracking(orderId, null),
                "Đã đồng bộ trạng thái thành công"
        ));
    }
}