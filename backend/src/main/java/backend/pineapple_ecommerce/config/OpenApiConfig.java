package backend.pineapple_ecommerce.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI hiển thị ô "Authorize" để nhập Bearer token.
 * Truy cập: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "Pineapple E-commerce API",
                version     = "1.0",
                description = "Backend API for Pineapple E-commerce platform"
        )
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
