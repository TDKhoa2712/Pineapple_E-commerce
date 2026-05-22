package backend.pineapple_ecommerce.modules.order.specification;

import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.modules.order.models.Order;
import backend.pineapple_ecommerce.common.enums.PaymentMethod;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * JPA Specification cho Order — cho phép kết hợp nhiều filter linh hoạt.
 * Cách dùng:
 * <pre>
 *   Specification<Order> spec = Specification
 *       .where(OrderSpecification.hasStatus(status))
 *       .and(OrderSpecification.hasUserId(userId))
 *       .and(OrderSpecification.createdBetween(from, to));
 *   orderRepository.findAll(spec, pageable);
 * </pre>
 */
public class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> hasUserId(Long userId) {
        return (root, query, cb) ->
                userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Order> hasPaymentMethod(PaymentMethod method) {
        return (root, query, cb) ->
                method == null ? null : cb.equal(root.get("paymentMethod"), method);
    }

    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from == null) return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            if (to   == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.between(root.get("createdAt"), from, to);
        };
    }
}
