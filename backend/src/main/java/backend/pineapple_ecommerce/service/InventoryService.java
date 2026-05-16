package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.dto.request.StockAdjustmentRequest;
import backend.pineapple_ecommerce.dto.response.InventoryBatchResponse;
import backend.pineapple_ecommerce.dto.response.InventorySummaryResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.StockAdjustmentResponse;

import java.util.List;

/**
 * Quản lý lô hàng tồn kho.
 */
public interface InventoryService {

    InventoryBatchResponse addBatch(CreateInventoryBatchRequest request);

    List<InventoryBatchResponse> getAvailableBatches(Long productId);

    List<InventoryBatchResponse> getAllBatchesByProduct(Long productId);

    InventoryBatchResponse getBatchById(Long batchId);

    /** Scheduled job — đánh dấu lô hết hạn (chạy 01:00 SA mỗi ngày). */
    void markExpiredBatches();

    /**
     * NEW — 2.3: Trigger thủ công markExpiredBatches cho Admin.
     * Trả về số lô đã được đánh dấu.
     */
    int markExpiredBatchesManual();

    int getTotalStock(Long productId);

    /**
     * NEW — 2.3: Danh sách lô sắp hết hạn trong N ngày tới.
     */
    List<InventoryBatchResponse> getExpiringSoon(int days);

    /**
     * NEW — 2.3: Tổng hợp tồn kho tất cả sản phẩm, phân trang.
     */
    PageResponse<InventorySummaryResponse> getInventorySummary(int page, int size);

    /**
     * NEW — 2.3: Điều chỉnh số lượng lô hàng kèm lý do (audit trail).
     * adjustmentQty: dương = thêm, âm = bớt.
     */
    StockAdjustmentResponse adjustBatch(Long batchId, Long adminUserId, StockAdjustmentRequest request);
}
