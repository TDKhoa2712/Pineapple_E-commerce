package backend.pineapple_ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Pineapple E-commerce Application
 *
 * @EnableJpaAuditing — bật tính năng tự động gán createdAt / updatedAt cho BaseEntity
 */

@SpringBootApplication
@EnableJpaAuditing
@EnableRetry
@EnableScheduling
@EnableAsync
public class PineappleEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PineappleEcommerceApplication.class, args);
    }
}