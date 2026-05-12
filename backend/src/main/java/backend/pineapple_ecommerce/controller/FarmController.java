package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.CreateFarmRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.FarmResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.service.FarmService;
import backend.pineapple_ecommerce.service.UserService;
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

@Tag(name = "Farms", description = "Quản lý trang trại")
@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;
    private final UserService userService;

    // ─────────────────────────────────────────────
    // PUBLIC — GET
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy tất cả trang trại phân trang (public)")
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

    // ─────────────────────────────────────────────
    // FARMER — quản lý trang trại của mình
    // ─────────────────────────────────────────────

    @Operation(summary = "Trang trại của tôi (Farmer)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getMyFarms() {
        Long ownerId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(farmService.getMyFarms(ownerId)));
    }

    @Operation(summary = "Tạo trang trại mới (Farmer/Admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> create(
            @Valid @RequestBody CreateFarmRequest request) {

        Long ownerId = userService.getCurrentUserId();
        FarmResponse response = farmService.createFarm(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo trang trại thành công"));
    }

    @Operation(summary = "Cập nhật trang trại (chủ trang trại hoặc Admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{farmId}")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FarmResponse>> update(
            @PathVariable Long farmId,
            @Valid @RequestBody CreateFarmRequest request) {

        Long requesterId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                farmService.updateFarm(farmId, requesterId, request), "Cập nhật thành công"));
    }

    @Operation(summary = "Xoá trang trại (chủ trang trại hoặc Admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{farmId}")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long farmId) {
        Long requesterId = userService.getCurrentUserId();
        farmService.deleteFarm(farmId, requesterId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá trang trại"));
    }
}