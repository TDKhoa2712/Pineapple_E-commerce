package backend.pineapple_ecommerce.config;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Khởi tạo Cloudinary bean từ CloudinaryProperties.
 * Inject Cloudinary ở bất kỳ đâu cần upload / delete ảnh.
 */
@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private final CloudinaryProperties cloudinaryProperties;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(Map.of(
                "cloud_name", cloudinaryProperties.getCloudName(),
                "api_key",    cloudinaryProperties.getApiKey(),
                "api_secret", cloudinaryProperties.getApiSecret(),
                "secure",     cloudinaryProperties.isSecure()
        ));
    }
}