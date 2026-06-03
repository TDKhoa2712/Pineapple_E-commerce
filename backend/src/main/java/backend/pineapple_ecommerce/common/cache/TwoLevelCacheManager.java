package backend.pineapple_ecommerce.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager caffeineCacheManager;
    private final RedisCacheManager redisCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String cacheEvictChannel;
    private final String instanceId;

    private final Map<String, TwoLevelCache> caches = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(CacheManager caffeineCacheManager,
                                RedisCacheManager redisCacheManager,
                                RedisTemplate<String, Object> redisTemplate,
                                String cacheEvictChannel,
                                String instanceId) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.redisTemplate = redisTemplate;
        this.cacheEvictChannel = cacheEvictChannel;
        this.instanceId = instanceId;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, k -> {
            org.springframework.cache.caffeine.CaffeineCache caffeineCache =
                    (org.springframework.cache.caffeine.CaffeineCache) caffeineCacheManager.getCache(k);
            org.springframework.data.redis.cache.RedisCache redisCache =
                    (org.springframework.data.redis.cache.RedisCache) redisCacheManager.getCache(k);

            return new TwoLevelCache(caffeineCache, redisCache, redisTemplate, cacheEvictChannel, instanceId);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return redisCacheManager.getCacheNames();
    }
}
