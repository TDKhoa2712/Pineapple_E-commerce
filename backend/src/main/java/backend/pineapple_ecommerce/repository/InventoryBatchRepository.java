package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.InventoryBatch;
import backend.pineapple_ecommerce.enums.BatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findByProductIdAndStatus(Long productId, BatchStatus status);

    /** FIX: Dùng cho markExpiredBatches() — lấy tất cả batch theo status, không lọc theo productId */
    List<InventoryBatch> findByStatus(BatchStatus status);

    @Query("SELECT SUM(b.remainingQuantity) FROM InventoryBatch b WHERE b.product.id = :productId AND b.status = 'AVAILABLE'")
    Integer getTotalAvailableStock(Long productId);

    Optional<InventoryBatch> findByBatchCode(String batchCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.id = :productId " +
            "AND b.status = :status ORDER BY b.expiryDate ASC NULLS LAST")
    List<InventoryBatch> findByProductIdAndStatusWithLock(
            @Param("productId") Long productId,
            @Param("status") BatchStatus status);
}
