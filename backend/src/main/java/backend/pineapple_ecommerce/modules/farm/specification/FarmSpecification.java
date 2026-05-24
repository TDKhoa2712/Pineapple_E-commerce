package backend.pineapple_ecommerce.modules.farm.specification;

import backend.pineapple_ecommerce.common.enums.FarmStatus;
import backend.pineapple_ecommerce.modules.farm.models.Farm;
import org.springframework.data.jpa.domain.Specification;

public class FarmSpecification {

    private FarmSpecification() {}

    public static Specification<Farm> hasStatus(FarmStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Farm> isDeleted(Boolean isDeleted) {
        return (root, query, cb) ->
                isDeleted == null ? null : cb.equal(root.get("isDeleted"), isDeleted);
    }
}
