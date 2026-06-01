package backend.pineapple_ecommerce.modules.order.service;

import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderResponse;
import backend.pineapple_ecommerce.modules.order.models.Order;
import java.math.BigDecimal;
import java.util.Optional;

public interface OrderInternalService {
    Optional<Order> findById(Long id);
    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
    boolean updateShippingFeeIfNeeded(Long orderId, BigDecimal shippingFee);
}
