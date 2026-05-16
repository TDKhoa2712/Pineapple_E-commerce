package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kết quả validate giỏ hàng trước khi checkout.
 * isValid = false nếu có bất kỳ warning nào.
 */
@Getter
@Builder
public class CartValidationResponse {

    /** true = giỏ hàng hợp lệ, có thể checkout */
    private boolean isValid;

    /** Danh sách cảnh báo — rỗng nếu isValid = true */
    private List<CartItemWarning> warnings;

    /** Tổng tiền ước tính (chỉ tính các item hợp lệ) */
    private BigDecimal estimatedTotal;

    @Getter
    @Builder
    public static class CartItemWarning {
        private Long    productId;
        private String  productName;

        /**
         * Loại cảnh báo:
         * - OUT_OF_STOCK: sản phẩm hết hàng
         * - INSUFFICIENT_STOCK: số lượng yêu cầu > tồn kho
         */
        private String  warningType;
        private String  message;
        private Integer requestedQty;
        private Integer availableQty;
    }
}
