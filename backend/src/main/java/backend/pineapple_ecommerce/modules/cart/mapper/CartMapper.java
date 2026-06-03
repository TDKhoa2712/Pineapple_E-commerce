// ===== File: mapper/CartMapper.java =====
package backend.pineapple_ecommerce.modules.cart.mapper;

import backend.pineapple_ecommerce.modules.cart.models.Cart;
import backend.pineapple_ecommerce.modules.cart.models.CartItem;
import backend.pineapple_ecommerce.modules.cart.dto.response.CartItemResponse;
import backend.pineapple_ecommerce.modules.cart.dto.response.CartResponse;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(target = "productId",        expression = "java(item.getProduct().getId())")
    @Mapping(target = "productName",      expression = "java(item.getProduct().getName())")
    @Mapping(target = "productSlug",      expression = "java(item.getProduct().getSlug())")
    @Mapping(target = "productThumbnail", expression = "java(item.getProduct().getThumbnail())")
    @Mapping(target = "unitPrice",        expression = "java(item.getProduct().getEffectivePrice())")
    @Mapping(target = "subtotal",         expression = "java(calcSubtotal(item))")
    // FIX: thêm productStatus để FE biết sản phẩm còn active không
    @Mapping(target = "productStatus",    expression = "java(item.getProduct().getStatus() != null ? item.getProduct().getStatus().name() : null)")
    // FIX: thêm productUnit để FE biết đơn vị tính của sản phẩm
    @Mapping(target = "productUnit",      expression = "java(item.getProduct().getUnit())")
    @Mapping(target = "availableStock",   ignore = true)  // Service tính từ batches
    CartItemResponse toItemResponse(CartItem item);

    List<CartItemResponse> toItemResponseList(List<CartItem> items);

    default CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = toItemResponseList(cart.getItems());
        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity).sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(total)
                .build();
    }

    // Helper
    default BigDecimal calcSubtotal(CartItem item) {
        return item.getProduct().getEffectivePrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}