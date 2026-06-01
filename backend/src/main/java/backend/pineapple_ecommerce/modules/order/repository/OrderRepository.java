package backend.pineapple_ecommerce.modules.order.repository;

import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.modules.order.models.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /** NEW: filter user + status cùng lúc — cho getMyOrders(status) */
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product
        WHERE o.id = :id
    """)
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    long countByStatus(OrderStatus status);

    /**
     * Kiểm tra user đã mua & nhận sản phẩm chưa — 1 query duy nhất.
     * Dùng trong ReviewServiceImpl để thay thế hasPurchased cũ (N+1 bug).
     */
    @Query("""
        SELECT COUNT(oi) > 0 FROM OrderItem oi
        WHERE oi.order.user.id = :userId
          AND oi.product.id   = :productId
          AND oi.order.status = 'DELIVERED'
    """)
    boolean existsByUserIdAndProductIdAndDelivered(
            @Param("userId")    Long userId,
            @Param("productId") Long productId);

    @Query("""
        SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi
        WHERE oi.order.user.id = :userId
          AND oi.product.id   = :productId
          AND oi.order.status = 'DELIVERED'
    """)
    long countDeliveredQuantityByUserIdAndProductId(
            @Param("userId")    Long userId,
            @Param("productId") Long productId);

    /** Lấy tất cả orderIds theo danh sách — dùng cho bulk update */
    @Query("SELECT o FROM Order o WHERE o.id IN :ids")
    java.util.List<Order> findAllByIdIn(@Param("ids") java.util.List<Long> ids);

    /**
     * Tìm đơn hàng dựa trên ClientOrderCode (Mã đơn hàng hệ thống gửi cho GHN)
     */
    Optional<Order> findById(Long id);

    java.util.List<Order> findAllByOrderByCreatedAtAsc();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status <> :cancelledStatus")
    java.math.BigDecimal sumTotalAmountByStatusNot(@Param("cancelledStatus") OrderStatus cancelledStatus);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    java.util.List<Object[]> countOrdersByStatus();

    @Query("""
        SELECT function('to_char', o.createdAt, 'YYYY-MM') as monthStr,
               SUM(o.totalAmount) as revenue,
               COUNT(o) as orderCount
        FROM Order o
        WHERE o.status <> :cancelledStatus
          AND o.createdAt >= :cutoff
        GROUP BY function('to_char', o.createdAt, 'YYYY-MM')
        ORDER BY monthStr ASC
    """)
    java.util.List<Object[]> getMonthlyRevenueAndCount(
            @Param("cutoff") java.time.LocalDateTime cutoff,
            @Param("cancelledStatus") OrderStatus cancelledStatus);
}
