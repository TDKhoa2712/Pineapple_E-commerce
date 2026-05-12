package backend.pineapple_ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Pineapple E-commerce Application
 *
 * @EnableJpaAuditing — bật tính năng tự động gán createdAt / updatedAt cho BaseEntity
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class PineappleEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PineappleEcommerceApplication.class, args);
    }
}