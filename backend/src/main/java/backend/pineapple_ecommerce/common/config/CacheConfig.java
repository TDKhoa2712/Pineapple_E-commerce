package backend.pineapple_ecommerce.common.config;

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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

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

            // Define specific TTLs for caches
            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
            cacheConfigurations.put("categories", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
            cacheConfigurations.put("products", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("products_related", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("products_by_ids", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));

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
                    new CaffeineCache("products_by_ids", Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(2)).build())
            );
            cacheManager.setCaches(caches);
            return cacheManager;
        }
    }
}
