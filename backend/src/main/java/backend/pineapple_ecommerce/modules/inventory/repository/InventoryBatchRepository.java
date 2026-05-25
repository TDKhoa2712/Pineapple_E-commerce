package backend.pineapple_ecommerce.modules.inventory.repository;

import backend.pineapple_ecommerce.common.enums.BatchStatus;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findByProductIdAndStatus(Long productId, BatchStatus status);

    List<InventoryBatch> findByStatus(BatchStatus status);

    @Query("SELECT SUM(b.remainingQuantity) FROM InventoryBatch b " +
           "WHERE b.product.id = :productId AND b.status = 'AVAILABLE'")
    Integer getTotalAvailableStock(@Param("productId") Long productId);

    Optional<InventoryBatch> findByBatchCode(String batchCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.id = :id")
    Optional<InventoryBatch> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b LEFT JOIN FETCH b.product " +
           "WHERE b.product.id = :productId AND b.status = :status " +
           "ORDER BY b.expiryDate ASC NULLS LAST")
    List<InventoryBatch> findByProductIdAndStatusWithLock(
            @Param("productId") Long productId,
            @Param("status")    BatchStatus status);

    // ─────────────────────────────────────────────
    // NEW — 2.3 InventoryService
    // ─────────────────────────────────────────────

    /**
     * Lấy danh sách lô sắp hết hạn trong N ngày tới.
     * threshold = LocalDate.now().plusDays(N)
     */
    @Query("""
        SELECT b FROM InventoryBatch b
        LEFT JOIN FETCH b.product
        LEFT JOIN FETCH b.farm
        WHERE b.status = 'AVAILABLE'
        AND b.expiryDate IS NOT NULL
        AND b.expiryDate <= :threshold
        ORDER BY b.expiryDate ASC
    """)
    List<InventoryBatch> findExpiringSoon(@Param("threshold") LocalDate threshold);

    /**
     * Tổng hợp tồn kho tất cả sản phẩm còn AVAILABLE.
     * Trả về [productId, productName, totalStock, batchCount].
     * Dùng trong InventoryServiceImpl.getInventorySummary()
     */
    @Query("""
        SELECT b.product.id, b.product.name,
               SUM(b.remainingQuantity),
               COUNT(b.id)
        FROM InventoryBatch b
        WHERE b.status = 'AVAILABLE'
        GROUP BY b.product.id, b.product.name
        ORDER BY SUM(b.remainingQuantity) ASC
    """)
    Page<Object[]> findInventorySummaryRaw(Pageable pageable);

    /**
     * Lấy các productId distinct của một farm — dùng cho getFarmProducts().
     */
    @Query("""
        SELECT DISTINCT b.product.id FROM InventoryBatch b
        WHERE b.farm.id = :farmId AND b.status = 'AVAILABLE'
    """)
    List<Long> findDistinctProductIdsByFarmId(@Param("farmId") Long farmId);

    /**
     * Lấy tất cả lô được nhập trong khoảng thời gian (theo createdAt).
     * Dùng cho báo cáo nhập/xuất kho.
     * LEFT JOIN FETCH product để tránh N+1.
     */
    @Query("""
        SELECT b FROM InventoryBatch b
        LEFT JOIN FETCH b.product
        WHERE (:from IS NULL OR b.createdAt >= :from)
          AND (:to   IS NULL OR b.createdAt <= :to)
        ORDER BY b.product.id ASC, b.createdAt ASC
    """)
    List<InventoryBatch> findByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Lấy tổng tồn kho khả dụng của các sản phẩm theo danh sách ID.
     */
    @Query("""
        SELECT b.product.id, SUM(b.remainingQuantity)
        FROM InventoryBatch b
        WHERE b.product.id IN :productIds AND b.status = 'AVAILABLE'
        GROUP BY b.product.id
    """)
    List<Object[]> getTotalAvailableStockByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * Tổng tồn kho khả dụng hiện tại của tất cả sản phẩm — dùng cho summary.
     */
    @Query("""
        SELECT COALESCE(SUM(b.remainingQuantity), 0)
        FROM InventoryBatch b
        WHERE b.status = 'AVAILABLE'
    """)
    long sumAllAvailableStock();
}
