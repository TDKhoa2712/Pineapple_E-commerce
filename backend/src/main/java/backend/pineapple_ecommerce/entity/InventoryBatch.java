package backend.pineapple_ecommerce.entity;

import backend.pineapple_ecommerce.enums.BatchStatus;
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
     * NEW — 2.3 InventoryService
     */
    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BatchStatus status = BatchStatus.AVAILABLE;

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
