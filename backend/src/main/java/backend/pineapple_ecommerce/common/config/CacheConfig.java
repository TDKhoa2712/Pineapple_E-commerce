package backend.pineapple_ecommerce.common.config;

import backend.pineapple_ecommerce.common.cache.CacheMessageListener;
import backend.pineapple_ecommerce.common.cache.TwoLevelCacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableCaching
public class CacheConfig {

    @Configuration
    @ConditionalOnProperty(name = "app.cache.type", havingValue = "hybrid")
    public static class HybridCacheConfig {

        private final String instanceId = UUID.randomUUID().toString();
        private static final String CACHE_EVICT_CHANNEL = "cache:evict";

        @Bean
        public RedisTemplate<String, Object> cacheRedisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                         RedisTemplate<String, Object> cacheRedisTemplate) {
            // L1 Caffeine Cache Manager
            SimpleCacheManager caffeineCacheManager = new SimpleCacheManager();
            List<CaffeineCache> caffeineCaches = List.of(
                    new CaffeineCache("categories", Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).build()),
                    new CaffeineCache("products", Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(2)).build()),
                    new CaffeineCache("products_related", Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(2)).build()),
                    new CaffeineCache("products_by_ids", Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(2)).build()),
                    new CaffeineCache("oauth2_codes", Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(2)).build()),
                    new CaffeineCache("user_details", Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(2)).build())
            );
            caffeineCacheManager.setCaches(caffeineCaches);
            caffeineCacheManager.initializeCaches();

            // L2 Redis Cache Manager
            RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(60))
                    .disableCachingNullValues()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
            cacheConfigurations.put("categories", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
            cacheConfigurations.put("products", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("products_related", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("products_by_ids", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("oauth2_codes", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(2)));
            cacheConfigurations.put("user_details", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(2)));

            RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultCacheConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .build();

            // TwoLevelCacheManager combining L1 & L2
            return new TwoLevelCacheManager(
                    caffeineCacheManager,
                    redisCacheManager,
                    cacheRedisTemplate,
                    CACHE_EVICT_CHANNEL,
                    instanceId
            );
        }

        @Bean
        public RedisMessageListenerContainer redisMessageListenerContainer(
                RedisConnectionFactory connectionFactory,
                CacheManager cacheManager,
                ObjectMapper objectMapper) {

            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);

            CacheMessageListener listener = new CacheMessageListener(cacheManager, objectMapper, instanceId);
            container.addMessageListener(listener, new ChannelTopic(CACHE_EVICT_CHANNEL));

            return container;
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "app.cache.type", havingValue = "redis")
    public static class RedisCacheConfig {

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
            RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(60))
                    .disableCachingNullValues()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
            cacheConfigurations.put("categories", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
            cacheConfigurations.put("products", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("products_related", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("products_by_ids", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("oauth2_codes", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(2)));
            cacheConfigurations.put("user_details", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(2)));

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultCacheConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .build();
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "app.cache.type", havingValue = "caffeine", matchIfMissing = true)
    public static class CaffeineCacheConfig {

        @Bean
        public CacheManager cacheManager() {
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            List<CaffeineCache> caches = List.of(
                    new CaffeineCache("categories", Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).build()),
                    new CaffeineCache("products", Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(2)).build()),
                    new CaffeineCache("products_related", Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(2)).build()),
                    new CaffeineCache("products_by_ids", Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(2)).build()),
                    new CaffeineCache("oauth2_codes", Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(2)).build()),
                    new CaffeineCache("user_details", Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(2)).build())
            );
            cacheManager.setCaches(caches);
            return cacheManager;
        }
    }
}
