package backend.pineapple_ecommerce.mapper;

import backend.pineapple_ecommerce.dto.request.CreateAddressRequest;
import backend.pineapple_ecommerce.dto.response.AddressResponse;
import backend.pineapple_ecommerce.entity.Address;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Address toEntity(CreateAddressRequest request);

    AddressResponse toResponse(Address address);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(CreateAddressRequest request, @MappingTarget Address address);
}
