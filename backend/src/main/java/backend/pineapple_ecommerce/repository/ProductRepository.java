package backend.pineapple_ecommerce.repository;

import backend.pineapple_ecommerce.entity.Product;
import backend.pineapple_ecommerce.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.slug = :slug AND p.status = 'ACTIVE'")
    Optional<Product> findActiveBySlugWithImages(String slug);

    @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.category " +
            "WHERE p.status = 'ACTIVE' " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))",
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE' " +
                    "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.category",
            countQuery = "SELECT COUNT(p) FROM Product p")
    Page<Product> findAllWithCategory(Pageable pageable);

    @EntityGraph("Product.withCategory")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

}
