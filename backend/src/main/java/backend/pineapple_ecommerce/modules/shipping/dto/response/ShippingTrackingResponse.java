package backend.pineapple_ecommerce.modules.shipping.dto.response;

import backend.pineapple_ecommerce.common.enums.CarrierCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin tracking vận đơn trả về cho user/admin.
 *
 * <p>Thay đổi so với phiên bản cũ:
 * <ul>
 *   <li>Thêm {@code carrierCode}, {@code carrierName}
 *   <li>Đổi {@code ghnOrderCode} → {@code externalOrderCode} (generic)
 *   <li>Thêm {@code rawStatus} trong StatusLogEntry (để debug)
 * </ul>
 */
@Getter
@Builder
public class ShippingTrackingResponse {

    private Long      orderId;

    /** Carrier xử lý vận đơn này */
    private CarrierCode carrierCode;
    private String      carrierName;

    /**
     * Mã vận đơn của carrier (tracking code).
     * User dùng mã này để tra cứu trên app/website của carrier.
     * GHN: "FFFNL9HH" | GHTK: "S12345678.1.2" | ...
     */
    private String externalOrderCode;

    /** Trạng thái normalize (enum name): "DELIVERED", "OUT_FOR_DELIVERY", ... */
    private String currentStatus;

    /** Nhãn trạng thái tiếng Việt: "Giao hàng thành công" */
    private String currentStatusLabel;

    private BigDecimal     shippingFee;
    private BigDecimal     totalFee;
    private LocalDateTime  expectedDeliveryTime;
    private String         failReason;

    private List<StatusLogEntry> statusHistory;

    private LocalDateTime createdOnCarrierAt;
    private LocalDateTime lastSyncAt;

    @Getter
    @Builder
    public static class StatusLogEntry {
        private String        status;       // enum name
        private String        statusLabel;  // tiếng Việt
        private String        rawStatus;    // trạng thái gốc từ carrier
        private LocalDateTime updatedAt;
    }
}