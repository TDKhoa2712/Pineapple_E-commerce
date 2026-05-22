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

    /**
     * Search đầy đủ: keyword + categoryId + minPrice + maxPrice + isOrganic.
     * Chỉ trả về sản phẩm ACTIVE.
     * Tất cả filter đều optional (null = bỏ qua).
     */
    @Query(value = """
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.status = 'ACTIVE'
            AND (:keyword    IS NULL OR :keyword    = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:minPrice   IS NULL OR p.price >= :minPrice)
            AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
            AND (:isOrganic  IS NULL OR p.isOrganic = :isOrganic)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.status = 'ACTIVE'
            AND (:keyword    IS NULL OR :keyword    = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:minPrice   IS NULL OR p.price >= :minPrice)
            AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
            AND (:isOrganic  IS NULL OR p.isOrganic = :isOrganic)
            """)
    Page<Product> searchProducts(
            @Param("keyword")    String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            @Param("isOrganic")  Boolean isOrganic,
            Pageable pageable);

    /**
     * NEW — 2.4: Search mở rộng thêm farmId và inStock filter.
     *
     * farmId  : lọc sản phẩm có ít nhất 1 batch của farm đó.
     * inStock : true = chỉ trả sản phẩm còn tồn kho > 0.
     */
    @Query(value = """
            SELECT DISTINCT p FROM Product p
            LEFT JOIN FETCH p.category
            LEFT JOIN p.batches b
            WHERE p.status = 'ACTIVE'
            AND (:keyword    IS NULL OR :keyword    = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:farmId     IS NULL OR b.farm.id = :farmId)
            AND (:minPrice   IS NULL OR p.price >= :minPrice)
            AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
            AND (:isOrganic  IS NULL OR p.isOrganic = :isOrganic)
            AND (CAST(:inStock AS boolean) IS NULL OR CAST(:inStock AS boolean) = false
                 OR EXISTS (
                     SELECT 1 FROM InventoryBatch ib
                     WHERE ib.product = p
                     AND ib.status = 'AVAILABLE'
                     AND ib.remainingQuantity > 0
                 ))
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p) FROM Product p
            LEFT JOIN p.batches b
            WHERE p.status = 'ACTIVE'
            AND (:keyword    IS NULL OR :keyword    = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:farmId     IS NULL OR b.farm.id = :farmId)
            AND (:minPrice   IS NULL OR p.price >= :minPrice)
            AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
            AND (:isOrganic  IS NULL OR p.isOrganic = :isOrganic)
            AND (CAST(:inStock AS boolean) IS NULL OR CAST(:inStock AS boolean) = false
                 OR EXISTS (
                     SELECT 1 FROM InventoryBatch ib
                     WHERE ib.product = p
                     AND ib.status = 'AVAILABLE'
                     AND ib.remainingQuantity > 0
                 ))
            """)
    Page<Product> searchProductsExtended(
            @Param("keyword")    String keyword,
            @Param("categoryId") Long categoryId,
            @Param("farmId")     Long farmId,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            @Param("isOrganic")  Boolean isOrganic,
            @Param("inStock")    Boolean inStock,
            Pageable pageable);

    /**
     * Best seller: sắp xếp theo tổng số lượng đã bán (join OrderItem).
     * Chỉ tính các đơn hàng đã DELIVERED.
     */
    @Query(value = """
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.status = 'ACTIVE'
            AND (:keyword    IS NULL OR :keyword    = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:minPrice   IS NULL OR p.price >= :minPrice)
            AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
            AND (:isOrganic  IS NULL OR p.isOrganic = :isOrganic)
            ORDER BY (
                SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi
                WHERE oi.product = p AND oi.order.status = 'DELIVERED'
            ) DESC
            """)
    Page<Product> searchProductsBestSeller(
            @Param("keyword")    String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            @Param("isOrganic")  Boolean isOrganic,
            Pageable pageable);

    /**
     * Admin search: lọc theo keyword + status (không bắt buộc ACTIVE).
     */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:status IS NULL OR p.status = :status)
            """)
    Page<Product> searchProductsForAdmin(
            @Param("keyword") String keyword,
            @Param("status")  ProductStatus status,
            Pageable pageable);

    /** Related products: cùng category, loại trừ sản phẩm hiện tại */
    @Query("""
            SELECT p FROM Product p
            WHERE p.category.id = :categoryId
            AND p.id <> :excludeId
            AND p.status = 'ACTIVE'
            """)
    Page<Product> findRelatedProducts(
            @Param("categoryId") Long categoryId,
            @Param("excludeId")  Long excludeId,
            Pageable pageable);

    List<Product> findAllById(Iterable<Long> ids);
}
