package backend.pineapple_ecommerce.modules.farm.specification;

import backend.pineapple_ecommerce.common.enums.FarmStatus;
import backend.pineapple_ecommerce.modules.farm.models.Farm;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;

public class FarmSpecification {

    private FarmSpecification() {}

    public static Specification<Farm> fetchOwner() {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("owner", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    public static Specification<Farm> hasStatus(FarmStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Farm> hasStatusIn(java.util.List<FarmStatus> statuses) {
        return (root, query, cb) ->
                statuses == null || statuses.isEmpty() ? null : root.get("status").in(statuses);
    }

    public static Specification<Farm> isDeleted(Boolean isDeleted) {
        return (root, query, cb) ->
                isDeleted == null ? null : cb.equal(root.get("isDeleted"), isDeleted);
    }

    public static Specification<Farm> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("location")), pattern),
                    cb.like(cb.lower(root.join("owner", jakarta.persistence.criteria.JoinType.LEFT).get("fullName")), pattern),
                    cb.like(cb.lower(root.join("owner", jakarta.persistence.criteria.JoinType.LEFT).get("email")), pattern)
            );
        };
    }
}
