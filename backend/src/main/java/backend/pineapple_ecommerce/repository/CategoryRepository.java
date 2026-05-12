package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Lấy danh mục gốc (không có parent)
    List<Category> findByParentIsNull();

    // Lấy con của 1 danh mục
    List<Category> findByParentId(Long parentId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL")
    List<Category> findAllRootWithChildren();
}
