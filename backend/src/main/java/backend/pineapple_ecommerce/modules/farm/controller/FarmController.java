package backend.pineapple_ecommerce.modules.farm.controller;

import backend.pineapple_ecommerce.modules.farm.service.FarmService;
import backend.pineapple_ecommerce.common.enums.FarmStatus;
import backend.pineapple_ecommerce.modules.farm.dto.request.CreateFarmRequest;
import backend.pineapple_ecommerce.modules.farm.dto.request.RejectFarmRequest;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.modules.farm.dto.response.FarmResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.product.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Farms", description = "Quản lý trang trại")
@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;
    private final UserService userService;

    // ─────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy tất cả trang trại ACTIVE phân trang (public)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FarmResponse>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(farmService.getAllFarms(page, size)));
    }

    @Operation(summary = "Lấy chi tiết trang trại theo ID (public)")
    @GetMapping("/{farmId}")
    public ResponseEntity<ApiResponse<FarmResponse>> getById(@PathVariable Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(farmService.getFarmById(farmId)));
    }

    @Operation(summary = "Lấy sản phẩm của farm (public)")
    @GetMapping("/{farmId}/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getFarmProducts(
            @PathVariable Long farmId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                farmService.getFarmProducts(farmId, page, size)));
    }

    // ─────────────────────────────────────────────
    // FARMER
    // ─────────────────────────────────────────────

    @Operation(summary = "Trang trại của tôi (User/Farmer/Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getMyFarms() {
        Long ownerId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(farmService.getMyFarms(ownerId)));
    }

    @Operation(summary = "Tạo trang trại mới (User/Farmer/Admin) — sẽ ở trạng thái PENDING_APPROVAL",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> create(
            @Valid @RequestBody CreateFarmRequest request) {
        Long ownerId = userService.getCurrentUserId();
        FarmResponse response = farmService.createFarm(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo trang trại thành công. Vui lòng chờ Admin duyệt."));
    }

    @Operation(summary = "Cập nhật trang trại (chủ trang trại hoặc Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{farmId}")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> update(
            @PathVariable Long farmId,
            @Valid @RequestBody CreateFarmRequest request) {
        Long requesterId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                farmService.updateFarm(farmId, requesterId, request), "Cập nhật thành công"));
    }

    @Operation(summary = "Upload ảnh trang trại (chủ trang trại hoặc Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/{farmId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> uploadImage(
            @PathVariable Long farmId,
            @RequestParam("file") MultipartFile file) {
        Long requesterId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                farmService.uploadFarmImage(farmId, requesterId, file), "Upload ảnh thành công"));
    }

    @Operation(summary = "Xoá trang trại — soft delete (chủ trang trại hoặc Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{farmId}")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long farmId) {
        Long requesterId = userService.getCurrentUserId();
        farmService.deleteFarm(farmId, requesterId);
        return ResponseEntity.ok(ApiResponse.success(null, "Da gui yeu cau/ngung hoat dong trang trai"));
    }

    @Operation(summary = "Xin phep ngung hoat dong trang trai (chu trang trai) hoac ngung ngay (Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{farmId}/request-deactivation")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> requestDeactivation(@PathVariable Long farmId) {
        Long requesterId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                farmService.requestDeactivation(farmId, requesterId), "Da gui yeu cau/ngung hoat dong trang trai"));
    }

    @Operation(summary = "Xin phep hoat dong lai trang trai (chu trang trai) hoac kich hoat ngay (Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{farmId}/request-reactivation")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> requestReactivation(@PathVariable Long farmId) {
        Long requesterId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                farmService.requestReactivation(farmId, requesterId), "Da gui yeu cau/kich hoat lai trang trai"));
    }

    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy tất cả trang trại (Admin) — filter đa điều kiện",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<FarmResponse>>> getAllAdmin(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) FarmStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success(
                farmService.getAllFarmsAdmin(page, size, status, keyword, sortBy, sortDirection)));
    }

    @Operation(summary = "Duyệt trang trại (Admin) — PENDING_APPROVAL → ACTIVE",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/admin/{farmId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> approveFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(
                farmService.approveFarm(farmId), "Đã duyệt trang trại"));
    }

    @Operation(summary = "Từ chối trang trại (Admin) — PENDING_APPROVAL → REJECTED",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/admin/{farmId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> rejectFarm(
            @PathVariable Long farmId,
            @Valid @RequestBody RejectFarmRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                farmService.rejectFarm(farmId, request.getReason()), "Đã từ chối trang trại"));
    }

    @Operation(summary = "Kích hoạt trang trại (Admin) — INACTIVE/REJECTED → ACTIVE",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/admin/{farmId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> activateFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(
                farmService.activateFarm(farmId), "Đã kích hoạt trang trại"));
    }

    @Operation(summary = "Vô hiệu hóa trang trại (Admin) — ACTIVE → INACTIVE",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/admin/{farmId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> deactivateFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(
                farmService.deactivateFarm(farmId), "Đã vô hiệu hóa trang trại"));
    }
}
