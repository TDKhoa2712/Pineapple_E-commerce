package backend.pineapple_ecommerce.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.io.IOException;

@Slf4j
public class CacheMessageListener implements MessageListener {

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final String instanceId;

    public CacheMessageListener(CacheManager cacheManager, ObjectMapper objectMapper, String instanceId) {
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CacheMessage cacheMessage = objectMapper.readValue(message.getBody(), CacheMessage.class);
            log.debug("Received cache eviction sync message: {}", cacheMessage);

            if (instanceId.equals(cacheMessage.getSenderId())) {
                log.trace("Ignoring cache eviction message sent by ourselves (instanceId: {})", instanceId);
                return;
            }

            if (cacheManager instanceof TwoLevelCacheManager) {
                TwoLevelCacheManager twoLevelCacheManager = (TwoLevelCacheManager) cacheManager;
                org.springframework.cache.Cache cache = twoLevelCacheManager.getCache(cacheMessage.getCacheName());
                if (cache instanceof TwoLevelCache) {
                    ((TwoLevelCache) cache).evictLocal(cacheMessage.getKey());
                }
            }
        } catch (IOException e) {
            log.error("Failed to deserialize CacheMessage from Redis channel", e);
        }
    }
}
