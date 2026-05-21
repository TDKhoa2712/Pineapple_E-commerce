package backend.pineapple_ecommerce.entity;

import backend.pineapple_ecommerce.enums.CarrierCode;
import backend.pineapple_ecommerce.enums.ShippingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Vận đơn giao hàng — thiết kế provider-agnostic.
 *
 * <p>Thay thế hoàn toàn {@code GhnShipment}. Hỗ trợ mọi carrier
 * bằng cách lưu:
 * <ul>
 *   <li>{@code carrierCode} — xác định carrier (GHN, GHTK, ...)
 *   <li>{@code externalOrderCode} — mã vận đơn do carrier cấp
 *   <li>{@code rawStatus} — trạng thái gốc từ carrier (để debug)
 *   <li>{@code currentStatus} — trạng thái đã normalize về {@link ShippingStatus}
 *   <li>{@code carrierMetadata} — JSON mở rộng lưu dữ liệu riêng của carrier
 *       (ví dụ: GHN sort_code, shopId; GHTK pick_work_shift, ...)
 * </ul>
 *
 * <p>Schema DB:
 * <pre>
 * CREATE TABLE shipments (
 *   id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
 *   order_id              BIGINT NOT NULL UNIQUE,
 *   carrier_code          VARCHAR(30) NOT NULL,
 *   external_order_code   VARCHAR(100),
 *   client_order_code     VARCHAR(100),
 *   current_status        VARCHAR(40) NOT NULL DEFAULT 'PENDING_PICKUP',
 *   raw_status            VARCHAR(100),
 *   shipping_fee          DECIMAL(10,2),
 *   insurance_fee         DECIMAL(10,2),
 *   total_fee             DECIMAL(10,2),
 *   expected_delivery_time DATETIME,
 *   last_sync_at          DATETIME,
 *   status_log            TEXT,
 *   fail_reason           VARCHAR(500),
 *   carrier_metadata      TEXT,          -- JSON
 *   created_on_carrier_at DATETIME,
 *   cancelled_at          DATETIME,
 *   created_at            DATETIME,
 *   updated_at            DATETIME
 * );
 * CREATE INDEX idx_shipments_order_id ON shipments(order_id);
 * CREATE INDEX idx_shipments_external_code ON shipments(external_order_code);
 * CREATE INDEX idx_shipments_carrier_status ON shipments(carrier_code, current_status);
 * </pre>
 */
@Entity
@Table(
        name = "shipments",
        indexes = {
                @Index(name = "idx_shipments_order_id",       columnList = "order_id"),
                @Index(name = "idx_shipments_external_code",  columnList = "external_order_code"),
                @Index(name = "idx_shipments_carrier_status", columnList = "carrier_code, current_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Shipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Liên kết với đơn hàng nội bộ (1-1) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // ── Thông tin carrier ────────────────────────────────────────────

    /** Đơn vị vận chuyển xử lý vận đơn này */
    @Enumerated(EnumType.STRING)
    @Column(name = "carrier_code", nullable = false, length = 30)
    private CarrierCode carrierCode;

    /**
     * Mã vận đơn do carrier cấp (tracking code).
     * GHN: "FFFNL9HH" | GHTK: "S12345678.1.2" | ViettelPost: "VV123456789VN"
     */
    @Column(name = "external_order_code", length = 100)
    private String externalOrderCode;

    /**
     * Mã đơn nội bộ gửi lên carrier.
     * Thường = orderId.toString() để dễ đối soát.
     */
    @Column(name = "client_order_code", length = 100)
    private String clientOrderCode;

    // ── Trạng thái ────────────────────────────────────────────────────

    /**
     * Trạng thái đã normalize về enum chuẩn nội bộ.
     * Frontend/Logic nghiệp vụ chỉ cần đọc field này.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 40)
    @Builder.Default
    private ShippingStatus currentStatus = ShippingStatus.PENDING_PICKUP;

    /**
     * Trạng thái gốc từ carrier (dùng để debug / audit).
     * GHN: "ready_to_pick" | GHTK: "Đang lấy hàng" | ...
     */
    @Column(name = "raw_status", length = 100)
    private String rawStatus;

    // ── Phí giao hàng ─────────────────────────────────────────────────

    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "insurance_fee", precision = 10, scale = 2)
    private BigDecimal insuranceFee;

    @Column(name = "total_fee", precision = 10, scale = 2)
    private BigDecimal totalFee;

    // ── Thời gian ─────────────────────────────────────────────────────

    @Column(name = "expected_delivery_time")
    private LocalDateTime expectedDeliveryTime;

    /** Lần cuối đồng bộ từ carrier (webhook hoặc scheduler) */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    /** Thời điểm vận đơn được tạo thành công phía carrier */
    @Column(name = "created_on_carrier_at")
    private LocalDateTime createdOnCarrierAt;

    /** Thời điểm hủy vận đơn (null nếu chưa hủy) */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ── Dữ liệu phụ trợ ───────────────────────────────────────────────

    /**
     * Lịch sử trạng thái dạng JSON (array of {status, updatedAt}).
     * Lưu dưới dạng jsonb để dễ dàng truy xuất timeline.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_log", columnDefinition = "jsonb")
    private String statusLog;

    /** Lý do giao thất bại (delivery_fail) nếu có */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "carrier_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String carrierMetadata;
    // ── Helpers ───────────────────────────────────────────────────────

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public boolean isActive() {
        return cancelledAt == null
                && externalOrderCode != null
                && !currentStatus.isTerminal();
    }
}