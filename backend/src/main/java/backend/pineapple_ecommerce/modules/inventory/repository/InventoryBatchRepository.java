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

    @Query("""
        SELECT b FROM InventoryBatch b
        LEFT JOIN FETCH b.product
        LEFT JOIN FETCH b.farm
        WHERE b.product.id = :productId
          AND b.status = :status
          AND (b.farm IS NULL OR b.farm.status = 'ACTIVE' OR b.farm.status = 'PENDING_DEACTIVATION')
        """)
    List<InventoryBatch> findByProductIdAndStatus(
            @Param("productId") Long productId,
            @Param("status")    BatchStatus status);

    List<InventoryBatch> findByStatus(BatchStatus status);

    @Query("SELECT SUM(b.remainingQuantity) FROM InventoryBatch b " +
           "WHERE b.product.id = :productId AND b.status = 'AVAILABLE' " +
           "AND (b.farm IS NULL OR b.farm.status = 'ACTIVE' OR b.farm.status = 'PENDING_DEACTIVATION')")
    Integer getTotalAvailableStock(@Param("productId") Long productId);

    Optional<InventoryBatch> findByBatchCode(String batchCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.id = :id")
    Optional<InventoryBatch> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b " +
           "WHERE b.product.id = :productId AND b.status = :status " +
           "AND (b.farm IS NULL OR EXISTS (SELECT 1 FROM Farm f WHERE f.id = b.farm.id AND (f.status = 'ACTIVE' OR f.status = 'PENDING_DEACTIVATION')))" +
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
     * Tổng hợp tồn kho tất cả sản phẩm, bao gồm sản phẩm có tồn kho bằng 0.
     * Trả về [productId, productName, totalStock, batchCount].
     */
    @Query(value = """
        SELECT p.id as productId, p.name as productName,
               COALESCE(SUM(CASE WHEN b.status = 'AVAILABLE' THEN b.remainingQuantity ELSE 0 END), 0) as totalStock,
               COUNT(CASE WHEN b.status = 'AVAILABLE' THEN b.id ELSE NULL END) as batchCount
        FROM Product p
        LEFT JOIN p.batches b
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        GROUP BY p.id, p.name
        """,
        countQuery = """
        SELECT COUNT(p) FROM Product p
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Object[]> findInventorySummaryRaw(
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * Lấy danh sách số lô và số lượng nhập theo từng sản phẩm trong khoảng thời gian.
     */
    @Query("""
        SELECT b.product.id as productId, b.product.name as productName,
               COUNT(b.id) as batchesImported,
               SUM(b.quantity) as quantityImported
        FROM InventoryBatch b
        WHERE (CAST(:from AS timestamp) IS NULL OR b.createdAt >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR b.createdAt <= :to)
        GROUP BY b.product.id, b.product.name
    """)
    List<Object[]> findImportReportRaw(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /**
     * Lấy tổng số lượng bán trong khoảng thời gian từ các đơn hàng hợp lệ.
     */
    @Query("""
        SELECT oi.product.id as productId, oi.productName as productName,
               SUM(oi.quantity) as quantitySold
        FROM OrderItem oi
        JOIN oi.order o
        WHERE (CAST(:from AS timestamp) IS NULL OR o.createdAt >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR o.createdAt <= :to)
          AND o.status NOT IN (
            backend.pineapple_ecommerce.common.enums.OrderStatus.CANCELLED,
            backend.pineapple_ecommerce.common.enums.OrderStatus.REFUNDED,
            backend.pineapple_ecommerce.common.enums.OrderStatus.RETURNED
          )
        GROUP BY oi.product.id, oi.productName
    """)
    List<Object[]> findSalesReportRaw(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /**
     * Lấy số lô và số lượng bị hết hạn trong khoảng thời gian.
     */
    @Query("""
        SELECT b.product.id as productId, b.product.name as productName,
               COUNT(b.id) as batchesExpired,
               SUM(b.remainingQuantity) as quantityExpired
        FROM InventoryBatch b
        WHERE b.status = 'EXPIRED'
          AND (CAST(:from AS date) IS NULL OR b.expiryDate >= :from)
          AND (CAST(:to   AS date) IS NULL OR b.expiryDate <= :to)
        GROUP BY b.product.id, b.product.name
    """)
    List<Object[]> findExpiryReportRaw(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);

    /**
     * Lấy ngày nhập hàng sớm nhất và muộn nhất của mỗi sản phẩm trong khoảng thời gian.
     */
    @Query("""
        SELECT b.product.id as productId,
               MIN(b.createdAt) as earliestImport,
               MAX(b.createdAt) as latestImport
        FROM InventoryBatch b
        WHERE (CAST(:from AS timestamp) IS NULL OR b.createdAt >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR b.createdAt <= :to)
        GROUP BY b.product.id
    """)
    List<Object[]> findImportDatesRaw(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /**
     * Lấy các productId distinct của một farm — dùng cho getFarmProducts().
     */
    @Query("""
        SELECT DISTINCT b.product.id FROM InventoryBatch b
        WHERE b.farm.id = :farmId AND b.status = 'AVAILABLE' AND (b.farm.status = 'ACTIVE' OR b.farm.status = 'PENDING_DEACTIVATION')
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
        WHERE (CAST(:from AS timestamp) IS NULL OR b.createdAt >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR b.createdAt <= :to)
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
        WHERE b.product.id IN :productIds
          AND b.status = 'AVAILABLE'
          AND (b.farm IS NULL OR b.farm.status = 'ACTIVE' OR b.farm.status = 'PENDING_DEACTIVATION')
        GROUP BY b.product.id
    """)
    List<Object[]> getTotalAvailableStockByProductIds(@Param("productIds") List<Long> productIds);

    @Query("""
        SELECT COUNT(b) > 0
        FROM InventoryBatch b
        WHERE b.product.id = :productId
          AND b.farm.owner.id = :ownerId
          AND b.farm.status NOT IN ('ACTIVE', 'PENDING_DEACTIVATION')
    """)
    boolean existsProductInNonActiveFarm(
            @Param("productId") Long productId,
            @Param("ownerId") Long ownerId);

    /**
     * Tổng tồn kho khả dụng hiện tại của tất cả sản phẩm — dùng cho summary.
     */
    @Query("""
        SELECT COALESCE(SUM(b.remainingQuantity), 0)
        FROM InventoryBatch b
        WHERE b.status = 'AVAILABLE'
          AND (b.farm IS NULL OR b.farm.status = 'ACTIVE' OR b.farm.status = 'PENDING_DEACTIVATION')
    """)
    long sumAllAvailableStock();

    @Query(value = """
        SELECT b FROM InventoryBatch b
        LEFT JOIN FETCH b.product p
        LEFT JOIN FETCH b.farm f
        WHERE (:status IS NULL OR b.status = :status)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(b.batchCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR (f IS NOT NULL AND LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          )
        """,
        countQuery = """
        SELECT COUNT(b) FROM InventoryBatch b
        LEFT JOIN b.product p
        LEFT JOIN b.farm f
        WHERE (:status IS NULL OR b.status = :status)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(b.batchCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR (f IS NOT NULL AND LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          )
        """)
    Page<InventoryBatch> findAllWithProductAndFarm(
            @Param("keyword") String keyword,
            @Param("status") BatchStatus status,
            Pageable pageable);

    /**
     * Lấy tổng lượng nhập theo ngày trong khoảng thời gian.
     */
    @Query("""
        SELECT CAST(b.createdAt as LocalDate) as dateVal, SUM(b.quantity) as qty
        FROM InventoryBatch b
        WHERE (CAST(:from AS timestamp) IS NULL OR b.createdAt >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR b.createdAt <= :to)
        GROUP BY CAST(b.createdAt as LocalDate)
        ORDER BY CAST(b.createdAt as LocalDate) ASC
    """)
    List<Object[]> findImportTimelineRaw(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /**
     * Lấy tổng lượng bán theo ngày trong khoảng thời gian.
     */
    @Query("""
        SELECT CAST(o.createdAt as LocalDate) as dateVal, SUM(oi.quantity) as qty
        FROM OrderItem oi
        JOIN oi.order o
        WHERE (CAST(:from AS timestamp) IS NULL OR o.createdAt >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR o.createdAt <= :to)
          AND o.status NOT IN (
            backend.pineapple_ecommerce.common.enums.OrderStatus.CANCELLED,
            backend.pineapple_ecommerce.common.enums.OrderStatus.REFUNDED,
            backend.pineapple_ecommerce.common.enums.OrderStatus.RETURNED
          )
        GROUP BY CAST(o.createdAt as LocalDate)
        ORDER BY CAST(o.createdAt as LocalDate) ASC
    """)
    List<Object[]> findSalesTimelineRaw(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query(value = """
        SELECT b FROM InventoryBatch b
        LEFT JOIN FETCH b.product p
        LEFT JOIN FETCH b.farm f
        WHERE b.farm.id = :farmId AND (:keyword IS NULL OR :keyword = ''
           OR LOWER(b.batchCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
        countQuery = """
        SELECT COUNT(b) FROM InventoryBatch b
        LEFT JOIN b.product p
        WHERE b.farm.id = :farmId AND (:keyword IS NULL OR :keyword = ''
           OR LOWER(b.batchCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<InventoryBatch> findAllByFarmIdAndKeyword(
            @Param("farmId") Long farmId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
