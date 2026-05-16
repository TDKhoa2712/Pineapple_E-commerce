package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo nhập/xuất kho theo khoảng thời gian.
 *
 * Cấu trúc:
 * - period: khoảng thời gian báo cáo
 * - summary: tổng hợp toàn kỳ
 * - details: chi tiết theo từng sản phẩm
 */
@Getter
@Builder
public class InventoryReportResponse {

    private LocalDate from;
    private LocalDate to;
    private ReportSummary summary;
    private List<ProductReportDetail> details;

    @Getter
    @Builder
    public static class ReportSummary {
        /** Tổng số lô được nhập trong kỳ */
        private long totalBatchesImported;
        /** Tổng số lượng nhập kho trong kỳ (kg/đơn vị) */
        private long totalQuantityImported;
        /** Tổng số lượng đã bán (xuất kho) trong kỳ */
        private long totalQuantitySold;
        /** Tổng số lô hết hạn trong kỳ */
        private long totalBatchesExpired;
        /** Tổng số lượng bị hủy do hết hạn */
        private long totalQuantityExpired;
        /** Tổng tồn kho khả dụng tại thời điểm xuất báo cáo */
        private long currentAvailableStock;
    }

    @Getter
    @Builder
    public static class ProductReportDetail {
        private Long productId;
        private String productName;

        /** Số lô nhập trong kỳ */
        private long batchesImported;
        /** Tổng số lượng nhập */
        private long quantityImported;
        /** Số lượng đã xuất (bán) = quantity - remainingQuantity của các lô trong kỳ */
        private long quantitySold;
        /** Số lô hết hạn */
        private long batchesExpired;
        /** Số lượng hết hạn bị hủy */
        private long quantityExpired;
        /** Tồn kho khả dụng hiện tại */
        private long currentStock;
        /** Ngày nhập kho sớm nhất trong kỳ */
        private LocalDate earliestImport;
        /** Ngày nhập kho muộn nhất trong kỳ */
        private LocalDate latestImport;
    }
}