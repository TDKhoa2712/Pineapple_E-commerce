package backend.pineapple_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Ghi lại mọi lần điều chỉnh tồn kho thủ công (mất mát, hỏng hóc, kiểm kê, nhầm lẫn...).
 * adjustmentQty dương = tăng thêm, âm = giảm bớt.
 */
@Entity
@Table(name = "stock_adjustments", indexes = {
    @Index(name = "idx_stock_adj_batch", columnList = "batch_id"),
    @Index(name = "idx_stock_adj_created", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class StockAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private InventoryBatch batch;

    /** Số lượng điều chỉnh: dương = thêm vào, âm = bớt đi */
    @Column(name = "adjustment_qty", nullable = false)
    private Integer adjustmentQty;

    /** Lý do điều chỉnh (bắt buộc để audit trail) */
    @Column(nullable = false, length = 500)
    private String reason;

    /** User thực hiện điều chỉnh (Admin/Farmer) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjusted_by")
    private User adjustedBy;

    /** Tồn kho trước khi điều chỉnh (snapshot để audit) */
    @Column(name = "qty_before", nullable = false)
    private Integer qtyBefore;

    /** Tồn kho sau khi điều chỉnh (snapshot để audit) */
    @Column(name = "qty_after", nullable = false)
    private Integer qtyAfter;
}
