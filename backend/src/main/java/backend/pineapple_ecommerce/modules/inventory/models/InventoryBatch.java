package backend.pineapple_ecommerce.modules.inventory.models;

import backend.pineapple_ecommerce.common.entity.BaseEntity;
import backend.pineapple_ecommerce.common.enums.BatchStatus;
import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.farm.models.Farm;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_batches", indexes = {
        @Index(name = "idx_batch_product_status", columnList = "product_id, status"),
        @Index(name = "idx_batch_expiry",         columnList = "expiry_date")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class InventoryBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Column(name = "batch_code", nullable = false, unique = true, length = 50)
    private String batchCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** Độ ngọt Brix — đặc trưng của dứa */
    @Column(name = "sweetness_level", precision = 4, scale = 2)
    private BigDecimal sweetnessLevel;

    /**
     * Ghi chú về lô hàng (lý do nhập, nguồn gốc đặc biệt, điều chỉnh...).
     * (Có thể dùng cho các nghiệp vụ mở rộng trong tương lai)
     */
    @Column(columnDefinition = "TEXT")
    private String note;

    /**
     * Lý do từ chối khi admin reject lô hàng của farmer nhập.
     * Chỉ có ý nghĩa khi status = REJECTED (hoặc PENDING_APPROVAL sau khi farmer gửi lại).
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BatchStatus status = BatchStatus.PENDING_APPROVAL;

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    public boolean hasStock(int requestedQty) {
        return this.remainingQuantity >= requestedQty;
    }

    public void deductStock(int qty) {
        this.remainingQuantity -= qty;
        if (this.remainingQuantity <= 0) {
            this.remainingQuantity = 0;
            this.status = BatchStatus.SOLD_OUT;
        }
    }
}
