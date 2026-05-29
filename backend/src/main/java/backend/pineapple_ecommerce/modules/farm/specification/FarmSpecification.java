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

    public static Specification<Farm> isDeleted(Boolean isDeleted) {
        return (root, query, cb) ->
                isDeleted == null ? null : cb.equal(root.get("isDeleted"), isDeleted);
    }
}
