package backend.pineapple_ecommerce.entity;

import backend.pineapple_ecommerce.enums.GhnShippingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lưu thông tin vận đơn GHN liên kết với Order.
 *
 * <p>Thiết kế 1-1 với Order:
 * - Một đơn hàng có tối đa một vận đơn GHN active.
 * - Nếu đơn bị hủy và tạo lại thì lưu lịch sử qua trường cancelledAt.
 *
 * <p>Trường quan trọng:
 * - ghnOrderCode: mã vận đơn GHN (tracking code) — dùng để tra cứu trạng thái
 * - clientOrderCode: mã đơn nội bộ gửi lên GHN (= orderId của hệ thống)
 * - shippingFee: phí ship tính từ GHN (đã lưu snapshot, không query lại)
 * - expectedDeliveryTime: thời gian giao dự kiến từ GHN
 * - currentStatus: trạng thái GHN hiện tại (sync qua webhook)
 * - lastSyncAt: lần cuối đồng bộ trạng thái từ GHN
 */
@Entity
@Table(
    name = "ghn_shipments",
    indexes = {
        @Index(name = "idx_ghn_order_id", columnList = "order_id"),
        @Index(name = "idx_ghn_order_code", columnList = "ghn_order_code")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class GhnShipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Liên kết với đơn hàng nội bộ */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /**
     * Mã vận đơn GHN (tracking code).
     * Ví dụ: "FFFNL9HH"
     * Dùng để: tra cứu trạng thái, in label, giao lại, hủy.
     */
    @Column(name = "ghn_order_code", length = 50)
    private String ghnOrderCode;

    /**
     * Mã đơn nội bộ gửi lên GHN.
     * Thường = orderId.toString() để dễ đối soát.
     */
    @Column(name = "client_order_code", length = 50)
    private String clientOrderCode;

    /** Sort code GHN (dùng cho nhãn in) */
    @Column(name = "sort_code", length = 20)
    private String sortCode;

    /** Trạng thái vận đơn hiện tại từ GHN */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", length = 40)
    @Builder.Default
    private GhnShippingStatus currentStatus = GhnShippingStatus.READY_TO_PICK;

    /** Phí giao hàng thực tế từ GHN (VNĐ) */
    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee;

    /** Phí bảo hiểm */
    @Column(name = "insurance_fee", precision = 10, scale = 2)
    private BigDecimal insuranceFee;

    /** Tổng phí (shipping + insurance + ...) */
    @Column(name = "total_fee", precision = 10, scale = 2)
    private BigDecimal totalFee;

    /** Thời gian giao dự kiến từ GHN */
    @Column(name = "expected_delivery_time")
    private LocalDateTime expectedDeliveryTime;

    /** Lần cuối đồng bộ trạng thái từ GHN (webhook hoặc polling) */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    /** Lần cuối sync log trạng thái đầy đủ (JSON) */
    @Column(name = "status_log", columnDefinition = "TEXT")
    private String statusLog;

    /**
     * Lý do giao thất bại (nếu có).
     * GHN trả về fail_reason khi status = delivery_fail.
     */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    /** Thời điểm tạo vận đơn GHN thành công */
    @Column(name = "created_on_ghn_at")
    private LocalDateTime createdOnGhnAt;

    /** Vận đơn đã bị hủy trên GHN chưa */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public boolean isActive() {
        return cancelledAt == null && ghnOrderCode != null;
    }
}
