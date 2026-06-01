package backend.pineapple_ecommerce.infrastructure.carrier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Tạo RestTemplate bean riêng cho GHN.
 * Tách biệt với RestTemplate dùng chung để dễ cấu hình timeout/interceptor riêng.
 */
@Configuration
public class GhnRestTemplateConfig {

    @Bean(name = "ghnRestTemplate")
    public RestTemplate ghnRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3s connect timeout
        factory.setReadTimeout(5000);    // 5s read timeout
        return new RestTemplate(factory);
    }
}
