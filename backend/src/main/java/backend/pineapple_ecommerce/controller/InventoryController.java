package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.InventoryBatchResponse;
import backend.pineapple_ecommerce.service.InventoryService;
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

@Tag(name = "Inventory", description = "Quản lý lô hàng tồn kho")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Nhập lô hàng mới cho sản phẩm")
    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> addBatch(
            @Valid @RequestBody CreateInventoryBatchRequest request) {

        InventoryBatchResponse response = inventoryService.addBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Nhập lô hàng thành công"));
    }

    @Operation(summary = "Lấy chi tiết lô hàng theo ID")
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> getBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getBatchById(batchId)));
    }

    @Operation(summary = "Lấy tất cả lô hàng AVAILABLE của sản phẩm")
    @GetMapping("/products/{productId}/available")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> getAvailable(
            @PathVariable Long productId) {

        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAvailableBatches(productId)));
    }

    @Operation(summary = "Lấy tất cả lô hàng của sản phẩm (mọi trạng thái)")
    @GetMapping("/products/{productId}/batches")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> getAllBatches(
            @PathVariable Long productId) {

        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllBatchesByProduct(productId)));
    }

    @Operation(summary = "Tổng tồn kho khả dụng của sản phẩm")
    @GetMapping("/products/{productId}/stock")
    public ResponseEntity<ApiResponse<Integer>> getTotalStock(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getTotalStock(productId)));
    }
}