package backend.pineapple_ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Tạo RestTemplate bean riêng cho GHN.
 * Tách biệt với RestTemplate dùng chung để dễ cấu hình timeout/interceptor riêng.
 */
@Configuration
public class GhnRestTemplateConfig {

    @Bean(name = "ghnRestTemplate")
    public RestTemplate ghnRestTemplate() {
        return new RestTemplate();
    }
}
