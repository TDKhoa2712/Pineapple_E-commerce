package backend.pineapple_ecommerce.modules.category.event;

import org.springframework.context.ApplicationEvent;

public class CategoryChangedEvent extends ApplicationEvent {
    public CategoryChangedEvent(Object source) {
        super(source);
    }
}
