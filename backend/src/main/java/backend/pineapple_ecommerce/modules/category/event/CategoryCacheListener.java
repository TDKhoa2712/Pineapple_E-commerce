package backend.pineapple_ecommerce.modules.category.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryCacheListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCategoryChange(CategoryChangedEvent event) {
        log.info("Handling CategoryChangedEvent: clearing categories cache (AFTER_COMMIT)");
        try {
            Cache categoriesCache = cacheManager.getCache("categories");
            if (categoriesCache != null) {
                categoriesCache.clear();
            }
        } catch (Exception e) {
            log.error("Failed to clear categories cache", e);
        }
    }
}
