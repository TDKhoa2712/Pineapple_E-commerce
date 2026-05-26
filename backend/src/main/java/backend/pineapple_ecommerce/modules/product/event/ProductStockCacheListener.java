package backend.pineapple_ecommerce.modules.product.event;

import backend.pineapple_ecommerce.modules.inventory.event.ProductStockChangedEvent;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
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
public class ProductStockCacheListener {

    private final CacheManager cacheManager;
    private final ProductRepository productRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockChange(ProductStockChangedEvent event) {
        Long productId = event.getProductId();
        log.info("Handling ProductStockChangedEvent: evicting cache for product id={} (AFTER_COMMIT)", productId);
        try {
            Cache productsCache = cacheManager.getCache("products");
            if (productsCache != null) {
                // Evict by product ID
                productsCache.evict(productId);
                
                // Fetch product to find its slug and evict by slug too
                productRepository.findById(productId).ifPresent(product -> {
                    if (product.getSlug() != null) {
                        productsCache.evict(product.getSlug());
                    }
                });
            }

            // Invalidate related caches
            Cache relatedCache = cacheManager.getCache("products_related");
            if (relatedCache != null) {
                relatedCache.clear();
            }

            Cache byIdsCache = cacheManager.getCache("products_by_ids");
            if (byIdsCache != null) {
                byIdsCache.clear();
            }
        } catch (Exception e) {
            log.error("Failed to evict product cache on stock change", e);
        }
    }
}
