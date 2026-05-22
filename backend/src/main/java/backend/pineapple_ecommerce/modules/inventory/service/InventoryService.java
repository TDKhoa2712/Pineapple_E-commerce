package backend.pineapple_ecommerce.modules.inventory.service;

import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import backend.pineapple_ecommerce.modules.inventory.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.modules.inventory.dto.request.StockAdjustmentRequest;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventoryBatchResponse;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventorySummaryResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.inventory.dto.response.StockAdjustmentResponse;
import backend.pineapple_ecommerce.modules.order.models.OrderItem;

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

    int markExpiredBatchesManual();

    int getTotalStock(Long productId);

    List<InventoryBatchResponse> getExpiringSoon(int days);

    PageResponse<InventorySummaryResponse> getInventorySummary(int page, int size);

    StockAdjustmentResponse adjustBatch(Long batchId, Long adminUserId, StockAdjustmentRequest request);

    // ─────────────────────────────────────────────
    // Order-domain operations
    // ─────────────────────────────────────────────

    /**
     * Trừ tồn kho theo chiến lược FIFO (First In First Out / gần hết hạn trước).
     * Trả về batch chính được dùng cho OrderItem.
     *
     * <p>Dùng pessimistic lock — đảm bảo không oversell khi nhiều đơn đặt cùng lúc.
     * Chỉ gọi từ bên trong @Transactional của OrderService.
     *
     * @param productId  sản phẩm cần trừ tồn kho
     * @param quantity   số lượng cần trừ
     * @return batch đầu tiên được trừ (dùng để lưu vào OrderItem.batch)
     * @throws BusinessException nếu tồn kho không đủ
     */
    InventoryBatch deductStockFifo(Long productId, int quantity);

    /**
     * Hoàn lại tồn kho khi đơn hàng bị huỷ hoặc yêu cầu hoàn tiền.
     *
     * <p>Chỉ hoàn cho các OrderItem có batch != null.
     * Nếu batch đang SOLD_OUT → chuyển về AVAILABLE sau khi hoàn.
     *
     * @param orderItems danh sách item của đơn hàng cần hoàn kho
     */
    void restoreStockForOrder(List<OrderItem> orderItems);

    // ─────────────────────────────────────────────
    // Farm-domain query
    // ─────────────────────────────────────────────

    /**
     * Lấy danh sách productId phân biệt có ít nhất 1 batch AVAILABLE của farm.
     * Dùng bởi FarmService để hiển thị sản phẩm của farm.
     *
     * @param farmId ID của farm
     * @return list productId, rỗng nếu farm chưa có sản phẩm nào
     */
    List<Long> getDistinctProductIdsByFarm(Long farmId);
}