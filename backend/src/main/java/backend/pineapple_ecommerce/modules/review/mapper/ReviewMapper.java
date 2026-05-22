package backend.pineapple_ecommerce.modules.review.mapper;

import backend.pineapple_ecommerce.modules.review.models.Review;
import backend.pineapple_ecommerce.modules.review.models.ReviewImage;
import backend.pineapple_ecommerce.modules.review.dto.request.CreateReviewRequest;
import backend.pineapple_ecommerce.modules.review.dto.response.ReviewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReviewMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "product",   ignore = true)
    @Mapping(target = "images",    ignore = true)
    @Mapping(target = "votes",     ignore = true)
    @Mapping(target = "isHidden",  ignore = true)
    @Mapping(target = "helpfulCount",   ignore = true)
    @Mapping(target = "unhelpfulCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Review toEntity(CreateReviewRequest request);

    @Mapping(target = "userId",        expression = "java(review.getUser().getId())")
    @Mapping(target = "userFullName",  expression = "java(review.getUser().getFullName())")
    @Mapping(target = "userAvatar",    expression = "java(review.getUser().getAvatar())")
    @Mapping(target = "productId",     expression = "java(review.getProduct().getId())")
    @Mapping(target = "imageUrls",     expression = "java(mapImageUrls(review))")
    @Mapping(target = "helpfulCount",  source = "helpfulCount")
    @Mapping(target = "unhelpfulCount",source = "unhelpfulCount")
    @Mapping(target = "isHidden",      source = "isHidden")
    ReviewResponse toResponse(Review review);

    List<ReviewResponse> toResponseList(List<Review> reviews);

    default List<String> mapImageUrls(Review review) {
        if (review.getImages() == null) return List.of();
        return review.getImages().stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());
    }
}
