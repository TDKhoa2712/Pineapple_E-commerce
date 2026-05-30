package backend.pineapple_ecommerce.modules.payment.repository;

import backend.pineapple_ecommerce.modules.payment.models.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    // Bổ sung hàm này để khóa dòng dữ liệu khi update
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.transactionCode = :txnRef")
    Optional<Payment> findByTransactionCodeForUpdate(@Param("txnRef") String txnRef);

    // Hàm hỗ trợ cho Cronjob tìm các đơn hàng bị kẹt
    @Query("SELECT p FROM Payment p WHERE p.status = 'UNPAID' AND p.provider != 'COD' AND p.transactionCode IS NOT NULL AND p.createdAt < :cutoff")
    List<Payment> findByStatusUnpaidAndCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}