package backend.pineapple_ecommerce.modules.auth.repository;

import backend.pineapple_ecommerce.modules.auth.models.Role;
import backend.pineapple_ecommerce.common.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
