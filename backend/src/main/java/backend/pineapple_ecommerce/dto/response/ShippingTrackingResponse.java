package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin tracking vận đơn trả về cho user.
 * Tổng hợp từ entity GhnShipment + order info.
 */
@Getter
@Builder
public class ShippingTrackingResponse {

    // ── Thông tin vận đơn ──────────────────────────────
    private Long orderId;
    private String ghnOrderCode;           // Mã vận đơn GHN để user tra cứu trên app GHN
    private String currentStatus;          // Code GHN: "delivering", "delivered", ...
    private String currentStatusLabel;     // Tiếng Việt: "Đang giao hàng", "Giao thành công"

    // ── Thông tin giao hàng ───────────────────────────
    private BigDecimal shippingFee;
    private BigDecimal totalFee;
    private LocalDateTime expectedDeliveryTime;
    private String failReason;             // Lý do giao thất bại (nếu có)

    // ── Lịch sử trạng thái ───────────────────────────
    private List<StatusLogEntry> statusHistory;

    // ── Thời gian ─────────────────────────────────────
    private LocalDateTime createdOnGhnAt;
    private LocalDateTime lastSyncAt;

    /**
     * Một mốc lịch sử trạng thái vận đơn.
     */
    @Getter
    @Builder
    public static class StatusLogEntry {
        private String status;
        private String statusLabel;
        private LocalDateTime updatedAt;
    }
}
