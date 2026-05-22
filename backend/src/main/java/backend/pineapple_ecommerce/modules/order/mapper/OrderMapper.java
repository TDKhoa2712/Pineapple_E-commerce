package backend.pineapple_ecommerce.modules.order.mapper;

import backend.pineapple_ecommerce.modules.order.models.Order;
import backend.pineapple_ecommerce.modules.order.models.OrderItem;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderItemResponse;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "productName")      // snapshot
    @Mapping(target = "productThumbnail", source = "productThumbnail")
        // snapshot
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "paymentStatus", expression = "java(order.getPaymentStatus().name())")
    @Mapping(target = "paymentMethod", expression = "java(order.getPaymentMethod().name())")
    @Mapping(target = "items", expression = "java(toItemResponseList(order.getItems()))")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);
}
