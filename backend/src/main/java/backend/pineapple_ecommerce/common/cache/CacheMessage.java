package backend.pineapple_ecommerce.common.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String cacheName;
    private Object key;
    private String senderId;
}
