package backend.pineapple_ecommerce.modules.order.mapper;

import backend.pineapple_ecommerce.modules.order.models.Order;
import backend.pineapple_ecommerce.modules.order.models.OrderItem;
import backend.pineapple_ecommerce.modules.order.dto.response.BatchAllocationResponse;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderItemResponse;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "productName")      // snapshot
    @Mapping(target = "productThumbnail", source = "productThumbnail")
    @Mapping(target = "batchId", source = "batch.id")
    @Mapping(target = "batchCode", source = "batchCode")
    @Mapping(target = "productUnit", source = "product.unit")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    default OrderResponse toResponse(Order order) {
        if (order == null) return null;

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            // Group by productId to aggregate split items
            Map<Long, List<OrderItem>> grouped = order.getItems().stream()
                    .collect(Collectors.groupingBy(item -> item.getProduct().getId()));

            for (Map.Entry<Long, List<OrderItem>> entry : grouped.entrySet()) {
                List<OrderItem> items = entry.getValue();
                OrderItem first = items.get(0);

                int totalQty = items.stream().mapToInt(OrderItem::getQuantity).sum();
                BigDecimal totalSubtotal = items.stream()
                        .map(OrderItem::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<BatchAllocationResponse> batches = items.stream()
                        .map(item -> BatchAllocationResponse.builder()
                                .batchId(item.getBatch() != null ? item.getBatch().getId() : null)
                                .batchCode(item.getBatchCode())
                                .quantity(item.getQuantity())
                                .build())
                        .toList();

                itemResponses.add(OrderItemResponse.builder()
                        .id(first.getId())
                        .productId(first.getProduct().getId())
                        .productName(first.getProductName())
                        .productThumbnail(first.getProductThumbnail())
                        .quantity(totalQty)
                        .unitPrice(first.getUnitPrice())
                        .subtotal(totalSubtotal)
                        .productUnit(first.getProduct() != null ? first.getProduct().getUnit() : null)
                        .batches(batches)
                        .build());
            }
        }

        return OrderResponse.builder()
                .id(order.getId())
                // FIX: populate user info từ order.user
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .userFullName(order.getUser() != null ? order.getUser().getFullName() : null)
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .shippingAddress(order.getShippingAddress())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                // FIX: populate couponCode
                .couponCode(order.getCouponCode())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    List<OrderResponse> toResponseList(List<Order> orders);
}
