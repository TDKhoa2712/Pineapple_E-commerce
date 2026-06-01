package backend.pineapple_ecommerce.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Callable;

@Slf4j
public class TwoLevelCache implements Cache {

    private final CaffeineCache caffeineCache;
    private final RedisCache redisCache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String cacheEvictChannel;
    private final String instanceId;

    public TwoLevelCache(CaffeineCache caffeineCache, RedisCache redisCache,
                         RedisTemplate<String, Object> redisTemplate,
                         String cacheEvictChannel, String instanceId) {
        this.caffeineCache = caffeineCache;
        this.redisCache = redisCache;
        this.redisTemplate = redisTemplate;
        this.cacheEvictChannel = cacheEvictChannel;
        this.instanceId = instanceId;
    }

    @Override
    public String getName() {
        return redisCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        // Check L1
        ValueWrapper wrapper = caffeineCache.get(key);
        if (wrapper != null) {
            log.trace("L1 (Caffeine) Cache HIT for key: {} in cache: {}", key, getName());
            return wrapper;
        }

        // Check L2
        wrapper = redisCache.get(key);
        if (wrapper != null) {
            log.trace("L2 (Redis) Cache HIT for key: {} in cache: {}. Syncing to L1.", key, getName());
            caffeineCache.put(key, wrapper.get());
            return wrapper;
        }

        log.trace("Cache MISS for key: {} in cache: {}", key, getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = caffeineCache.get(key, type);
        if (value != null) {
            log.trace("L1 (Caffeine) Cache HIT for key: {} in cache: {}", key, getName());
            return value;
        }

        value = redisCache.get(key, type);
        if (value != null) {
            log.trace("L2 (Redis) Cache HIT for key: {} in cache: {}. Syncing to L1.", key, getName());
            caffeineCache.put(key, value);
            return value;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = caffeineCache.get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }

        wrapper = redisCache.get(key);
        if (wrapper != null) {
            T val = (T) wrapper.get();
            caffeineCache.put(key, val);
            return val;
        }

        // Both miss, load and cache
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        redisCache.put(key, value);
        caffeineCache.put(key, value);
        publishEviction(key);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper wrapper = redisCache.putIfAbsent(key, value);
        if (wrapper == null) {
            caffeineCache.put(key, value);
            publishEviction(key);
            return null;
        }
        caffeineCache.put(key, wrapper.get());
        return wrapper;
    }

    @Override
    public void evict(Object key) {
        redisCache.evict(key);
        caffeineCache.evict(key);
        publishEviction(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean evicted = redisCache.evictIfPresent(key);
        caffeineCache.evict(key);
        publishEviction(key);
        return evicted;
    }

    @Override
    public void clear() {
        redisCache.clear();
        caffeineCache.clear();
        publishEviction(null);
    }

    @Override
    public boolean invalidate() {
        boolean redisInvalidated = redisCache.invalidate();
        caffeineCache.clear();
        publishEviction(null);
        return redisInvalidated;
    }

    /**
     * Evict from Caffeine cache locally without publishing eviction messages to avoid infinite loops.
     */
    public void evictLocal(Object key) {
        if (key == null) {
            log.debug("Local clearing Caffeine cache: {}", getName());
            caffeineCache.clear();
            return;
        }

        log.debug("Local evicting Caffeine cache key: {} in cache: {}", key, getName());
        caffeineCache.evict(key);

        // Scan existing keys in L1 Caffeine to handle potential Jackson serialization type mismatches (e.g. Integer vs Long)
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        for (Object existingKey : nativeCache.asMap().keySet()) {
            if (keysMatch(existingKey, key)) {
                caffeineCache.evict(existingKey);
            }
        }
    }

    private boolean keysMatch(Object existingKey, Object incomingKey) {
        if (existingKey == null || incomingKey == null) {
            return false;
        }
        if (existingKey.equals(incomingKey)) {
            return true;
        }
        if (existingKey instanceof Number && incomingKey instanceof Number) {
            return ((Number) existingKey).longValue() == ((Number) incomingKey).longValue();
        }
        return existingKey.toString().equals(incomingKey.toString());
    }

    private void publishEviction(Object key) {
        try {
            CacheMessage message = new CacheMessage(getName(), key, instanceId);
            redisTemplate.convertAndSend(cacheEvictChannel, message);
            log.debug("Published cache eviction message: {}", message);
        } catch (Exception e) {
            log.error("Failed to publish cache eviction message for key: {} in cache: {}", key, getName(), e);
        }
    }
}
