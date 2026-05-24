package backend.pineapple_ecommerce.modules.user.specification;

import backend.pineapple_ecommerce.common.enums.UserStatus;
import backend.pineapple_ecommerce.modules.user.models.User;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<User> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;

            String cleanKeyword = keyword.trim().toLowerCase();

            Expression<String> unaccentEmail   = cb.function("unaccent", String.class, cb.lower(root.get("email")));
            Expression<String> unaccentName    = cb.function("unaccent", String.class, cb.lower(root.get("fullName")));
            Expression<String> unaccentKw1     = cb.function("unaccent", String.class, cb.literal(cleanKeyword));
            Expression<String> unaccentKw2     = cb.function("unaccent", String.class, cb.literal(cleanKeyword));

            Expression<String> pattern1 = cb.concat(cb.concat(cb.literal("%"), unaccentKw1), cb.literal("%"));
            Expression<String> pattern2 = cb.concat(cb.concat(cb.literal("%"), unaccentKw2), cb.literal("%"));

            return cb.or(
                    cb.like(unaccentEmail, pattern1),
                    cb.like(unaccentName,  pattern2)
            );
        };
    }
}
