package backend.pineapple_ecommerce.modules.order.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {
    private Long id;

    // FIX: thêm user info để FE hiển thị (OrderResponse của FE kỳ vọng userId, userEmail, userFullName)
    private Long   userId;
    private String userEmail;
    private String userFullName;

    private String status;
    private String paymentStatus;
    private String paymentMethod;

    private String shippingAddress;

    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private String note;

    // FIX: thêm couponCode để FE hiển thị mã giảm giá đã dùng
    private String couponCode;

    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
