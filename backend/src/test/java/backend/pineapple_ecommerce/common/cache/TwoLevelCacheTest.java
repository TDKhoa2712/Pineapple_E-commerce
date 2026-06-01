package backend.pineapple_ecommerce.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TwoLevelCacheTest {

    private CaffeineCache caffeineCache;
    private RedisCache redisCache;
    private RedisTemplate<String, Object> redisTemplate;
    private final String cacheEvictChannel = "cache:evict";
    private final String instanceId = "test-instance-1";

    private TwoLevelCache twoLevelCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        caffeineCache = mock(CaffeineCache.class);
        redisCache = mock(RedisCache.class);
        redisTemplate = mock(RedisTemplate.class);

        when(redisCache.getName()).thenReturn("test-cache");

        twoLevelCache = new TwoLevelCache(caffeineCache, redisCache, redisTemplate, cacheEvictChannel, instanceId);
    }

    @Test
    void testGetName() {
        assertEquals("test-cache", twoLevelCache.getName());
    }

    @Test
    void testGetHitL1() {
        org.springframework.cache.Cache.ValueWrapper wrapper = mock(org.springframework.cache.Cache.ValueWrapper.class);
        when(caffeineCache.get("key1")).thenReturn(wrapper);

        org.springframework.cache.Cache.ValueWrapper result = twoLevelCache.get("key1");

        assertNotNull(result);
        assertEquals(wrapper, result);
        verify(redisCache, never()).get(any());
    }

    @Test
    void testGetMissL1HitL2() {
        org.springframework.cache.Cache.ValueWrapper wrapper = mock(org.springframework.cache.Cache.ValueWrapper.class);
        when(wrapper.get()).thenReturn("value1");
        when(caffeineCache.get("key1")).thenReturn(null);
        when(redisCache.get("key1")).thenReturn(wrapper);

        org.springframework.cache.Cache.ValueWrapper result = twoLevelCache.get("key1");

        assertNotNull(result);
        assertEquals(wrapper, result);
        verify(caffeineCache).put("key1", "value1");
    }

    @Test
    void testPut() {
        twoLevelCache.put("key1", "value1");

        verify(redisCache).put("key1", "value1");
        verify(caffeineCache).put("key1", "value1");

        ArgumentCaptor<CacheMessage> captor = ArgumentCaptor.forClass(CacheMessage.class);
        verify(redisTemplate).convertAndSend(eq(cacheEvictChannel), captor.capture());

        CacheMessage message = captor.getValue();
        assertEquals("test-cache", message.getCacheName());
        assertEquals("key1", message.getKey());
        assertEquals(instanceId, message.getSenderId());
    }

    @Test
    void testEvict() {
        twoLevelCache.evict("key1");

        verify(redisCache).evict("key1");
        verify(caffeineCache).evict("key1");

        verify(redisTemplate).convertAndSend(eq(cacheEvictChannel), any(CacheMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEvictLocal() {
        com.github.benmanes.caffeine.cache.Cache nativeCache = mock(com.github.benmanes.caffeine.cache.Cache.class);
        ConcurrentMap map = mock(ConcurrentMap.class);

        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        when(nativeCache.asMap()).thenReturn(map);
        when(map.keySet()).thenReturn(Collections.emptySet());

        twoLevelCache.evictLocal("key1");

        verify(caffeineCache).evict("key1");
        verify(redisCache, never()).evict("key1");
        verify(redisTemplate, never()).convertAndSend(any(), any());
    }
}
