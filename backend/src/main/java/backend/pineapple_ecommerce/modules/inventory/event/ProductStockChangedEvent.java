package backend.pineapple_ecommerce.modules.inventory.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProductStockChangedEvent extends ApplicationEvent {
    private final Long productId;

    public ProductStockChangedEvent(Object source, Long productId) {
        super(source);
        this.productId = productId;
    }
}
