package backend.pineapple_ecommerce.modules.wishlist.mapper;

import backend.pineapple_ecommerce.modules.wishlist.models.Wishlist;
import backend.pineapple_ecommerce.modules.wishlist.dto.response.WishlistResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WishlistMapper {

    @Mapping(target = "productId", expression = "java(wl.getProduct().getId())")
    @Mapping(target = "productName", expression = "java(wl.getProduct().getName())")
    @Mapping(target = "productSlug", expression = "java(wl.getProduct().getSlug())")
    @Mapping(target = "productThumbnail", expression = "java(wl.getProduct().getThumbnail())")
    @Mapping(target = "productPrice", expression = "java(wl.getProduct().getPrice())")
    @Mapping(target = "productDiscountPrice", expression = "java(wl.getProduct().getDiscountPrice())")
    @Mapping(target = "productStatus", expression = "java(wl.getProduct().getStatus().name())")
    @Mapping(target = "productUnit", expression = "java(wl.getProduct().getUnit())")
    WishlistResponse toResponse(Wishlist wl);

    List<WishlistResponse> toResponseList(List<Wishlist> wishlists);
}
