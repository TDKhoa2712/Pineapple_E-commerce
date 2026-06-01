package backend.pineapple_ecommerce.common.init;

import backend.pineapple_ecommerce.modules.auth.models.Role;
import backend.pineapple_ecommerce.common.enums.RoleName;
import backend.pineapple_ecommerce.modules.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Tự động tạo các Role mặc định khi ứng dụng khởi động lần đầu.
 * Idempotent — an toàn khi chạy nhiều lần.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Created role: {}", roleName);
            }
        }
    }
}
