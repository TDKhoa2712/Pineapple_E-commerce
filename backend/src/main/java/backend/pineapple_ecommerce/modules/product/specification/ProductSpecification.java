package backend.pineapple_ecommerce.modules.product.specification;

import backend.pineapple_ecommerce.common.enums.BatchStatus;
import backend.pineapple_ecommerce.common.enums.FarmStatus;
import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.common.enums.ProductStatus;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import backend.pineapple_ecommerce.modules.order.models.OrderItem;
import backend.pineapple_ecommerce.modules.product.models.Product;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> fetchCategory() {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("category", JoinType.LEFT);
                query.distinct(true);  // thêm dòng này
            }
            return null;
        };
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;
            return cb.or(
                    cb.equal(root.get("category").get("id"), categoryId),
                    cb.equal(root.get("category").get("parent").get("id"), categoryId)
            );
        };
    }

    public static Specification<Product> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) ->
                minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) ->
                maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> isOrganic(Boolean isOrganic) {
        return (root, query, cb) ->
                isOrganic == null ? null : cb.equal(root.get("isOrganic"), isOrganic);
    }

    public static Specification<Product> inStock(Boolean inStock) {
        return (root, query, cb) -> {
            if (inStock == null || !inStock) return null;

            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<InventoryBatch> subRoot = subquery.from(InventoryBatch.class);
            subquery.select(cb.literal(1))
                    .where(
                            cb.equal(subRoot.get("product"), root),
                            cb.equal(subRoot.get("status"), BatchStatus.AVAILABLE),
                            cb.greaterThan(subRoot.get("remainingQuantity"), 0),
                            cb.or(
                                    cb.isNull(subRoot.get("farm")),
                                    subRoot.get("farm").get("status").in(FarmStatus.ACTIVE, FarmStatus.PENDING_DEACTIVATION)
                            )
                    );
            return cb.exists(subquery);
        };
    }

    public static Specification<Product> hasFarmId(Long farmId) {
        return (root, query, cb) -> {
            if (farmId == null) return null;

            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<InventoryBatch> subRoot = subquery.from(InventoryBatch.class);
            subquery.select(cb.literal(1))
                    .where(
                            cb.equal(subRoot.get("product"), root),
                            cb.equal(subRoot.get("farm").get("id"), farmId),
                            subRoot.get("farm").get("status").in(FarmStatus.ACTIVE, FarmStatus.PENDING_DEACTIVATION)
                    );
            return cb.exists(subquery);
        };
    }

    public static Specification<Product> searchByKeyword(String keyword, boolean sortByRelevance) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;

            String cleanKeyword = keyword.trim().toLowerCase();

            // PostgreSQL unaccent and lower for accent-insensitive search
            Expression<String> unaccentName = cb.function("unaccent", String.class, cb.lower(root.get("name")));
            Expression<String> unaccentKeyword = cb.function("unaccent", String.class, cb.literal(cleanKeyword));

            // Trigram Index matches text with LIKE/ILIKE using %wildcard%
            Predicate likePredicate = cb.like(unaccentName, cb.concat(cb.concat("%", unaccentKeyword), "%"));

            // Ranking by similarity (sorting by similarity DESC)
            if (sortByRelevance && query.getResultType() != Long.class && query.getResultType() != long.class) {
                Expression<Double> similarity = cb.function("similarity", Double.class, unaccentName, unaccentKeyword);
                query.orderBy(cb.desc(similarity));
            }

            return likePredicate;
        };
    }

    public static Specification<Product> sortByBestSeller() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<OrderItem> subRoot = subquery.from(OrderItem.class);
                subquery.select(cb.coalesce(cb.sum(subRoot.get("quantity")), 0L))
                        .where(
                                cb.equal(subRoot.get("product"), root),
                                cb.equal(subRoot.get("order").get("status"), OrderStatus.DELIVERED)
                        );
                query.orderBy(cb.desc(subquery));
            }
            return null;
        };
    }

    public static Specification<Product> createdByUser(Long userId) {
        return (root, query, cb) ->
                userId == null ? null : cb.equal(root.get("createdBy").get("id"), userId);
    }
}
