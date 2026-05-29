package backend.pineapple_ecommerce.modules.product.repository;

import backend.pineapple_ecommerce.common.enums.ProductStatus;
import backend.pineapple_ecommerce.modules.product.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.slug = :slug AND p.status = 'ACTIVE'")
    Optional<Product> findActiveBySlugWithImages(@Param("slug") String slug);

    /** Related products: cùng category, loại trừ sản phẩm hiện tại */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.category.id = :categoryId
            AND p.id <> :excludeId
            AND p.status = 'ACTIVE'
            """)
    Page<Product> findRelatedProducts(
            @Param("categoryId") Long categoryId,
            @Param("excludeId")  Long excludeId,
            Pageable pageable);

    @Override
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id IN :ids")
    List<Product> findAllById(@Param("ids") Iterable<Long> ids);
}
