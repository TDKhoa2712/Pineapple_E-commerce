package backend.pineapple_ecommerce.mapper;

import backend.pineapple_ecommerce.dto.request.CreateFarmRequest;
import backend.pineapple_ecommerce.dto.response.FarmResponse;
import backend.pineapple_ecommerce.entity.Farm;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FarmMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)   // set trong Service
    @Mapping(target = "batches", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Farm toEntity(CreateFarmRequest request);

    @Mapping(target = "ownerId", expression = "java(farm.getOwner().getId())")
    @Mapping(target = "ownerName", expression = "java(farm.getOwner().getFullName())")
    FarmResponse toResponse(Farm farm);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(CreateFarmRequest request, @MappingTarget Farm farm);
}
