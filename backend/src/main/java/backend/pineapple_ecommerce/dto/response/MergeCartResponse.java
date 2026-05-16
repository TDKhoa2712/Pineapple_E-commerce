package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Kết quả gộp giỏ hàng.
 * FE dùng để hiển thị thông báo nếu có sản phẩm bị bỏ qua.
 */
@Getter
@Builder
public class MergeCartResponse {

    /** Giỏ hàng sau khi merge */
    private CartResponse cart;

    /** Số sản phẩm được merge thành công */
    private int mergedCount;

    /** Danh sách sản phẩm bị bỏ qua và lý do */
    private List<SkippedItem> skippedItems;

    @Getter
    @Builder
    public static class SkippedItem {
        private Long productId;
        private String productName;
        private String reason; // "OUT_OF_STOCK" | "PRODUCT_INACTIVE" | "STOCK_CAPPED"
        private String message;
        /** Số lượng FE yêu cầu */
        private int requestedQty;
        /** Số lượng thực sự được thêm (0 nếu bị skip hoàn toàn, < requestedQty nếu bị cap) */
        private int actualQty;
    }
}