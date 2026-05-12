package backend.pineapple_ecommerce.mapper;

import backend.pineapple_ecommerce.dto.request.RegisterRequest;
import backend.pineapple_ecommerce.dto.response.UserResponse;
import backend.pineapple_ecommerce.entity.Role;
import backend.pineapple_ecommerce.entity.User;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    /**
     * RegisterRequest → User entity
     * password sẽ được encode trong Service, không map ở đây
     */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "password",  ignore = true)   // encode trong Service
    @Mapping(target = "status",    ignore = true)   // default ACTIVE
    @Mapping(target = "roles",     ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "cart",      ignore = true)
    @Mapping(target = "orders",    ignore = true)
    @Mapping(target = "wishlistItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    /**
     * User entity → UserResponse DTO
     * roles: Set<Role> → Set<String> (tên role)
     */
    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserResponse toResponse(User user);

    // Helper: Set<Role> → Set<String>
    default Set<String> mapRoles(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
    }
}
