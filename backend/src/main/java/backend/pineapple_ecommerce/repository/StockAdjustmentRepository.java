package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    /** Lịch sử điều chỉnh của một batch cụ thể */
    List<StockAdjustment> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    /** Lịch sử điều chỉnh phân trang cho Admin */
    Page<StockAdjustment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
