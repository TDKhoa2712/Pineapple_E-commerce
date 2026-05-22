package backend.pineapple_ecommerce.modules.category.mapper;

import backend.pineapple_ecommerce.modules.category.models.Category;
import backend.pineapple_ecommerce.modules.category.dto.request.CreateCategoryRequest;
import backend.pineapple_ecommerce.modules.category.dto.response.CategoryResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    // 1. Đặt tên cho phương thức toResponse bằng @Named
    @Named("simpleResponse")
    @Mapping(target = "parentId", expression = "java(category.getParent() != null ? category.getParent().getId() : null)")
    @Mapping(target = "parentName", expression = "java(category.getParent() != null ? category.getParent().getName() : null)")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toResponse(Category category);

    // 2. Chỉ định rõ rằng khi map List children, hãy sử dụng phương thức "simpleResponse"
    @Mapping(target = "parentId", expression = "java(category.getParent() != null ? category.getParent().getId() : null)")
    @Mapping(target = "parentName", expression = "java(category.getParent() != null ? category.getParent().getName() : null)")
    @Mapping(target = "children", source = "children", qualifiedByName = "simpleResponse")
    CategoryResponse toResponseWithChildren(Category category);

    // 3. Sử dụng qualifiedByName cho cả List response
    @IterableMapping(qualifiedByName = "simpleResponse")
    List<CategoryResponse> toResponseList(List<Category> categories);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(CreateCategoryRequest request, @MappingTarget Category category);
}