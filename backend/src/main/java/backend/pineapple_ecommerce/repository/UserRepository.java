package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    // ─────────────────────────────────────────────
    // ADMIN QUERIES
    // ─────────────────────────────────────────────

    /**
     * Tìm kiếm + lọc user cho trang Admin.
     * - keyword: tìm theo email hoặc fullName (ILIKE — case-insensitive)
     * - status:  lọc theo trạng thái; nếu null → không lọc
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:status IS NULL OR u.status = :status)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<User> findByStatusAndKeyword(
            @Param("status")  UserStatus status,
            @Param("keyword") String     keyword,
            Pageable pageable);
}
