package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.dto.response.InventoryBatchResponse;

import java.util.List;

/**
 * Quản lý lô hàng tồn kho.
 * Hỗ trợ mô hình multi-batch: một sản phẩm có nhiều lô với ngày thu hoạch, hạn sử dụng khác nhau.
 */
public interface InventoryService {

    /**
     * Nhập lô hàng mới cho sản phẩm.
     * remainingQuantity = quantity lúc khởi tạo.
     */
    InventoryBatchResponse addBatch(CreateInventoryBatchRequest request);

    /** Lấy tất cả lô hàng AVAILABLE của sản phẩm (dùng khi đặt hàng). */
    List<InventoryBatchResponse> getAvailableBatches(Long productId);

    /** Lấy tất cả lô hàng của sản phẩm (bao gồm SOLD_OUT, EXPIRED) — Admin. */
    List<InventoryBatchResponse> getAllBatchesByProduct(Long productId);

    /** Lấy chi tiết một lô hàng. */
    InventoryBatchResponse getBatchById(Long batchId);

    /**
     * Cập nhật trạng thái lô hàng (ví dụ: đánh dấu EXPIRED khi quá hạn).
     * Có thể gọi từ scheduled job.
     */
    void markExpiredBatches();

    /**
     * Tính tổng tồn kho khả dụng của sản phẩm.
     * SUM(remainingQuantity) WHERE status = AVAILABLE.
     */
    int getTotalStock(Long productId);
}
