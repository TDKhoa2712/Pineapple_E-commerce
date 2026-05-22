package backend.pineapple_ecommerce.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Danh sách trạng thái vận đơn GHN.
 * Ref: https://api.ghn.vn/home/docs/detail?id=48
 *
 * <p>Mapping sang OrderStatus nội bộ:
 * <pre>
 *   ready_to_pick    → PROCESSING
 *   picking          → PROCESSING
 *   picked           → PROCESSING
 *   storing          → PROCESSING
 *   transporting     → SHIPPING
 *   delivering       → SHIPPING
 *   delivered        → DELIVERED
 *   delivery_fail    → SHIPPING (cần retry)
 *   waiting_to_return→ RETURNED (đang chờ hoàn)
 *   return           → RETURNED
 *   returned         → RETURNED
 *   cancel           → CANCELLED
 * </pre>
 */
public enum GhnShippingStatus {

    READY_TO_PICK("ready_to_pick", "Chờ lấy hàng"),
    PICKING("picking", "Đang lấy hàng"),
    PICKED("picked", "Đã lấy hàng"),
    MONEY_COLLECT_PICKING("money_collect_picking", "Thu tiền khi lấy hàng"),
    STORING("storing", "Đang lưu kho"),
    TRANSPORTING("transporting", "Đang vận chuyển"),
    SORTING("sorting", "Đang phân loại"),
    DELIVERING("delivering", "Đang giao hàng"),
    MONEY_COLLECT_DELIVERING("money_collect_delivering", "Thu tiền khi giao hàng"),
    DELIVERED("delivered", "Giao hàng thành công"),
    DELIVERY_FAIL("delivery_fail", "Giao hàng thất bại"),
    WAITING_TO_RETURN("waiting_to_return", "Đang chờ trả hàng"),
    RETURN("return", "Đang trả hàng"),
    RETURNED("returned", "Đã trả hàng về shop"),
    EXCEPTION("exception", "Hàng ngoại lệ / vấn đề"),
    DAMAGE("damage", "Hàng hư hỏng"),
    LOST("lost", "Hàng thất lạc"),
    CANCEL("cancel", "Đã hủy vận đơn"),
    UNKNOWN("unknown", "Trạng thái không xác định");

    private final String code;
    private final String description;

    GhnShippingStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static GhnShippingStatus fromCode(String code) {
        if (code == null) return UNKNOWN;
        for (GhnShippingStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) return s;
        }
        return UNKNOWN;
    }

    /**
     * Map trạng thái GHN → OrderStatus nội bộ của hệ thống.
     */
    public OrderStatus toOrderStatus() {
        return switch (this) {
            case READY_TO_PICK, PICKING, PICKED, STORING, SORTING, MONEY_COLLECT_PICKING
                    -> OrderStatus.PROCESSING;
            case TRANSPORTING, DELIVERING, MONEY_COLLECT_DELIVERING, DELIVERY_FAIL
                    -> OrderStatus.SHIPPING;
            case DELIVERED
                    -> OrderStatus.DELIVERED;
            case WAITING_TO_RETURN, RETURN, RETURNED
                    -> OrderStatus.RETURNED;
            case CANCEL
                    -> OrderStatus.CANCELLED;
            default -> null; // Không tự động cập nhật với exception/damage/lost — cần xử lý thủ công
        };
    }
}
