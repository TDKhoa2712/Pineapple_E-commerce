package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.Role;
import backend.pineapple_ecommerce.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
