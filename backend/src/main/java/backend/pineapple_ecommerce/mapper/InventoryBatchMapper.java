package backend.pineapple_ecommerce.mapper;

import backend.pineapple_ecommerce.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.dto.response.InventoryBatchResponse;
import backend.pineapple_ecommerce.entity.InventoryBatch;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryBatchMapper {

    @Mapping(target = "id",                ignore = true)
    @Mapping(target = "product",           ignore = true)   // set trong Service
    @Mapping(target = "farm",              ignore = true)
    @Mapping(target = "remainingQuantity", ignore = true)   // = quantity lúc tạo
    @Mapping(target = "status",            ignore = true)
    @Mapping(target = "createdAt",         ignore = true)
    @Mapping(target = "updatedAt",         ignore = true)
    InventoryBatch toEntity(CreateInventoryBatchRequest request);

    @Mapping(target = "productId",   expression = "java(batch.getProduct().getId())")
    @Mapping(target = "productName", expression = "java(batch.getProduct().getName())")
    @Mapping(target = "farmId",      expression = "java(batch.getFarm() != null ? batch.getFarm().getId() : null)")
    @Mapping(target = "farmName",    expression = "java(batch.getFarm() != null ? batch.getFarm().getName() : null)")
    @Mapping(target = "status",      expression = "java(batch.getStatus().name())")
    InventoryBatchResponse toResponse(InventoryBatch batch);
}

