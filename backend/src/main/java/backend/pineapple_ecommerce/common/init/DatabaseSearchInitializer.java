package backend.pineapple_ecommerce.common.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSearchInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing PostgreSQL database search extensions and indexes...");

        // 1. Check and enable "unaccent" extension
        ensureExtension("unaccent");

        // 2. Check and enable "pg_trgm" extension
        ensureExtension("pg_trgm");

        // 3. Create the GIN Trigram functional index on products
        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_products_name_unaccent_trgm ON products USING GIN (unaccent(LOWER(name)) gin_trgm_ops)");
            log.info("Successfully verified/created GIN trigram index on products(unaccent(lower(name)))");
        } catch (Exception e) {
            log.error("Failed to create GIN trigram index on products. Please verify your table structure and permissions: {}", e.getMessage(), e);
        }
    }

    private void ensureExtension(String extensionName) {
        boolean exists = checkExtensionExists(extensionName);
        if (!exists) {
            log.info("Extension '{}' is missing. Attempting to create it...", extensionName);
            try {
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"" + extensionName + "\"");
                log.info("Successfully created extension '{}'", extensionName);
            } catch (Exception e) {
                // Check once again in case of concurrent initialization or other conditions
                if (checkExtensionExists(extensionName)) {
                    log.info("Extension '{}' was created by another process.", extensionName);
                    return;
                }
                log.error("Failed to create extension '{}': {}. The application requires this extension for search function. " +
                        "Please ask your DBA to run: CREATE EXTENSION IF NOT EXISTS \"{}\";", extensionName, e.getMessage(), extensionName);
                throw new RuntimeException("Missing database extension '" + extensionName + "' and failed to create it automatically.", e);
            }
        } else {
            log.info("Extension '{}' is already enabled.", extensionName);
        }
    }

    private boolean checkExtensionExists(String extensionName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_extension WHERE extname = ?",
                    Integer.class,
                    extensionName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Failed to check pg_extension table: {}. Assuming extension is missing.", e.getMessage());
            return false;
        }
    }
}
