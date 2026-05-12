package backend.pineapple_ecommerce.mapper;

import backend.pineapple_ecommerce.dto.request.CreateProductRequest;
import backend.pineapple_ecommerce.dto.request.UpdateProductRequest;
import backend.pineapple_ecommerce.dto.response.ProductDetailResponse;
import backend.pineapple_ecommerce.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.entity.Product;
import backend.pineapple_ecommerce.entity.ProductImage;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)   // gen trong Service
    @Mapping(target = "category", ignore = true)   // set trong Service
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "batches", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "wishlistItems", ignore = true)
    @Mapping(target = "status", ignore = true)   // default ACTIVE
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(CreateProductRequest request);

    // Summary: dùng trong list, search
    @Mapping(target = "categoryName", expression = "java(product.getCategory() != null ? product.getCategory().getName() : null)")
    @Mapping(target = "effectivePrice", expression = "java(product.getEffectivePrice())")
    @Mapping(target = "status", expression = "java(product.getStatus().name())")
    @Mapping(target = "totalStock", ignore = true)   // Service tính từ batches
    @Mapping(target = "averageRating", ignore = true)
    // Service tính từ reviews
    ProductSummaryResponse toSummaryResponse(Product product);

    // Detail: dùng trong trang chi tiết
    @Mapping(target = "categoryId", expression = "java(product.getCategory() != null ? product.getCategory().getId() : null)")
    @Mapping(target = "categoryName", expression = "java(product.getCategory() != null ? product.getCategory().getName() : null)")
    @Mapping(target = "effectivePrice", expression = "java(product.getEffectivePrice())")
    @Mapping(target = "status", expression = "java(product.getStatus().name())")
    @Mapping(target = "imageUrls", expression = "java(mapImageUrls(product))")
    @Mapping(target = "totalStock", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    ProductDetailResponse toDetailResponse(Product product);

    List<ProductSummaryResponse> toSummaryResponseList(List<Product> products);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "slug", ignore = true)
    void updateFromRequest(UpdateProductRequest request, @MappingTarget Product product);

    // Helper: Product → list URL ảnh
    default List<String> mapImageUrls(Product product) {
        if (product.getImages() == null) return List.of();
        return product.getImages().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());
    }
}
