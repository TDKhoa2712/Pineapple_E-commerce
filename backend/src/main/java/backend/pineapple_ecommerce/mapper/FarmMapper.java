package backend.pineapple_ecommerce.mapper;

import backend.pineapple_ecommerce.dto.request.CreateFarmRequest;
import backend.pineapple_ecommerce.dto.response.FarmResponse;
import backend.pineapple_ecommerce.entity.Farm;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FarmMapper {

    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "owner",           ignore = true)
    @Mapping(target = "batches",         ignore = true)
    @Mapping(target = "status",          ignore = true)   // mặc định PENDING_APPROVAL
    @Mapping(target = "isDeleted",       ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "imagePublicId",   ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    Farm toEntity(CreateFarmRequest request);

    @Mapping(target = "ownerId",         expression = "java(farm.getOwner().getId())")
    @Mapping(target = "ownerName",       expression = "java(farm.getOwner().getFullName())")
    @Mapping(target = "status",          expression = "java(farm.getStatus().name())")
    @Mapping(target = "rejectionReason", source = "rejectionReason")
    FarmResponse toResponse(Farm farm);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status",          ignore = true)   // status chỉ thay đổi qua approve/reject
    @Mapping(target = "isDeleted",       ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    void updateFromRequest(CreateFarmRequest request, @MappingTarget Farm farm);
}
