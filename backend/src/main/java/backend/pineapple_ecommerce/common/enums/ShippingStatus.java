package backend.pineapple_ecommerce.common.enums;

/**
 * Trạng thái vận đơn đã được chuẩn hóa, KHÔNG phụ thuộc vào bất kỳ carrier nào.
 *
 * <p>Mỗi carrier (GHN, GHTK, ViettelPost, ...) sẽ có class riêng để map
 * trạng thái thô của họ về enum này.
 *
 * <p>Mapping sang OrderStatus nội bộ:
 * <pre>
 *   PENDING_PICKUP   → PROCESSING
 *   PICKING_UP       → PROCESSING
 *   IN_TRANSIT       → SHIPPING
 *   OUT_FOR_DELIVERY → SHIPPING
 *   DELIVERY_FAILED  → SHIPPING  (sẽ retry)
 *   DELIVERED        → DELIVERED
 *   RETURNING        → RETURNED
 *   RETURNED         → RETURNED
 *   CANCELLED        → CANCELLED
 *   EXCEPTION        → (xử lý thủ công)
 * </pre>
 */
public enum ShippingStatus {

    // ── Giai đoạn lấy hàng ──────────────────────────────────────────
    PENDING_PICKUP("Chờ lấy hàng"),
    PICKING_UP("Đang lấy hàng"),
    PICKED_UP("Đã lấy hàng"),

    // ── Giai đoạn vận chuyển ────────────────────────────────────────
    IN_TRANSIT("Đang vận chuyển"),
    AT_WAREHOUSE("Đang ở kho trung chuyển"),
    SORTING("Đang phân loại"),

    // ── Giai đoạn giao hàng ─────────────────────────────────────────
    OUT_FOR_DELIVERY("Đang giao hàng"),
    DELIVERY_FAILED("Giao hàng thất bại"),
    DELIVERED("Giao hàng thành công"),

    // ── Hoàn hàng ────────────────────────────────────────────────────
    RETURNING("Đang hoàn hàng về shop"),
    RETURNED("Đã hoàn hàng về shop"),

    // ── Hủy / Ngoại lệ ───────────────────────────────────────────────
    CANCELLED("Đã hủy vận đơn"),
    EXCEPTION("Hàng ngoại lệ — cần xử lý thủ công"),
    LOST("Hàng thất lạc"),
    DAMAGED("Hàng hư hỏng"),

    UNKNOWN("Trạng thái không xác định");

    private final String description;

    ShippingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Map sang OrderStatus nội bộ của hệ thống.
     * Trả về null nếu không cần tự động cập nhật (xử lý thủ công).
     */
    public OrderStatus toOrderStatus() {
        return switch (this) {
            case PENDING_PICKUP, PICKING_UP, PICKED_UP, AT_WAREHOUSE, SORTING
                    -> OrderStatus.PROCESSING;
            case IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERY_FAILED
                    -> OrderStatus.SHIPPING;
            case DELIVERED
                    -> OrderStatus.DELIVERED;
            case RETURNING, RETURNED
                    -> OrderStatus.RETURNED;
            case CANCELLED
                    -> OrderStatus.CANCELLED;
            default -> null; // EXCEPTION, LOST, DAMAGED, UNKNOWN → xử lý thủ công
        };
    }

    /** Kiểm tra xem đây có phải trạng thái cuối (terminal) không. */
    public boolean isTerminal() {
        return switch (this) {
            case DELIVERED, RETURNED, CANCELLED, LOST, DAMAGED -> true;
            default -> false;
        };
    }
}