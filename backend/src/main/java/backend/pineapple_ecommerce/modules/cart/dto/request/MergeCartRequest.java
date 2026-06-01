package backend.pineapple_ecommerce.modules.cart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request gộp giỏ hàng khách (localStorage) vào giỏ hàng thật sau khi đăng nhập.
 *
 * FE gọi ngay sau khi nhận được JWT token thành công:
 * POST /api/v1/cart/merge
 * Body: { "items": [{ "productId": 1, "quantity": 2 }, ...] }
 *
 * Chiến lược merge (SMART MERGE):
 * - Nếu sản phẩm đã có trong giỏ → cộng dồn số lượng (không reset)
 * - Nếu tổng vượt tồn kho → cap lại bằng số tồn kho khả dụng
 * - Sản phẩm không còn active → bỏ qua, ghi vào skipped list
 * - Sản phẩm hết hàng → bỏ qua, ghi vào skipped list
 */
@Getter
@Setter
public class MergeCartRequest {

    @NotNull(message = "Danh sách sản phẩm không được null")
    @Valid
    private List<CartItemMerge> items;

    @Getter
    @Setter
    public static class CartItemMerge {

        @NotNull(message = "productId không được để trống")
        private Long productId;

        @NotNull(message = "quantity không được để trống")
        @Positive(message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
    }
}