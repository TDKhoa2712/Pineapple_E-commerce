package backend.pineapple_ecommerce.modules.inventory.controller;

import backend.pineapple_ecommerce.modules.inventory.dto.response.InventoryBatchResponse;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventoryReportResponse;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventorySummaryResponse;
import backend.pineapple_ecommerce.modules.inventory.dto.response.StockAdjustmentResponse;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.inventory.service.InventoryReportService;
import backend.pineapple_ecommerce.modules.inventory.service.InventoryService;
import backend.pineapple_ecommerce.modules.inventory.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.modules.inventory.dto.request.StockAdjustmentRequest;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Inventory", description = "Quan ly lo hang ton kho")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserService      userService;
    private final InventoryReportService inventoryReportService;

    @Operation(summary = "Nhap lo hang moi cho san pham")
    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> addBatch(
            @Valid @RequestBody CreateInventoryBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.addBatch(request), "Nhap lo hang thanh cong"));
    }

    @Operation(summary = "Lay chi tiet lo hang theo ID")
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> getBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getBatchById(batchId)));
    }

    @Operation(summary = "Lay tat ca lo hang AVAILABLE cua san pham")
    @GetMapping("/products/{productId}/available")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> getAvailable(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAvailableBatches(productId)));
    }

    @Operation(summary = "Lay tat ca lo hang cua san pham (moi trang thai)")
    @GetMapping("/products/{productId}/batches")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> getAllBatches(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllBatchesByProduct(productId)));
    }

    @Operation(summary = "Lấy danh sách lô hàng của trang trại phân trang (Farmer/Admin)")
    @GetMapping("/farms/{farmId}/batches")
    public ResponseEntity<ApiResponse<PageResponse<InventoryBatchResponse>>> getFarmBatches(
            @PathVariable Long farmId,
            @RequestParam(required = false)     String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)     String sortBy,
            @RequestParam(required = false)     String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getFarmBatches(farmId, keyword, page, size, sortBy, sortDirection)));
    }

    @Operation(summary = "Tong ton kho kha dung cua san pham")
    @GetMapping("/products/{productId}/stock")
    public ResponseEntity<ApiResponse<Integer>> getTotalStock(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getTotalStock(productId)));
    }

    // NEW 2.3
    @Operation(summary = "Danh sach lo sap het han (default: 7 ngay)")
    @GetMapping("/batches/expiring-soon")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> getExpiringSoon(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getExpiringSoon(days)));
    }

    @Operation(summary = "Tong hop ton kho tat ca san pham")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PageResponse<InventorySummaryResponse>>> getSummary(
            @RequestParam(required = false)     String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)     String sortBy,
            @RequestParam(required = false)     String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventorySummary(keyword, page, size, sortBy, sortDirection)));
    }

    @Operation(summary = "Dieu chinh so luong lo hang kem ly do")
    @PostMapping("/batches/{batchId}/adjust")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> adjustBatch(
            @PathVariable Long batchId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        Long adminId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.adjustBatch(batchId, adminId, request), "Dieu chinh ton kho thanh cong"));
    }

    @Operation(summary = "Lấy lịch sử điều chỉnh của lô hàng")
    @GetMapping("/batches/{batchId}/adjustments")
    public ResponseEntity<ApiResponse<List<StockAdjustmentResponse>>> getBatchAdjustments(
            @PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getBatchAdjustments(batchId)));
    }

    @Operation(summary = "Trigger thu cong danh dau lo het han (Admin)")
    @PostMapping("/admin/mark-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> triggerMarkExpired() {
        int count = inventoryService.markExpiredBatchesManual();
        return ResponseEntity.ok(ApiResponse.success(Map.of("markedCount", count), "Da danh dau " + count + " lo het han"));
    }

    /**
     * Báo cáo nhập/xuất kho theo khoảng thời gian.
     *
     * GET /api/v1/inventory/report?from=2025-01-01&to=2025-03-31
     *
     * Nếu không truyền from/to → báo cáo toàn bộ thời gian.
     * Trả về:
     *   - summary: tổng số lô nhập, tổng số lượng nhập/xuất/hết hạn, tồn kho hiện tại
     *   - details: chi tiết theo từng sản phẩm
     */
    @Operation(summary = "Báo cáo nhập/xuất kho theo khoảng thời gian")
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<InventoryReportResponse>> getInventoryReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String groupBy) {

        InventoryReportResponse report = inventoryReportService.generateReport(from, to, groupBy);
        String message = String.format(
                "Báo cáo từ %s đến %s — %d sản phẩm",
                from != null ? from : "đầu kỳ",
                to   != null ? to   : "hiện tại",
                report.getDetails().size());
        return ResponseEntity.ok(ApiResponse.success(report, message));
    }

    @Operation(summary = "Lấy tất cả lô hàng phân trang (Admin)")
    @GetMapping("/admin/batches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryBatchResponse>>> getAllBatches(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllBatches(keyword, page, size, sortBy, sortDirection)));
    }
}

