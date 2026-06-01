package backend.pineapple_ecommerce.modules.coupon.mapper;

import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponUsageResponse;
import backend.pineapple_ecommerce.modules.coupon.models.Coupon;
import backend.pineapple_ecommerce.modules.coupon.models.CouponUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CouponMapper {

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByEmail", source = "createdBy.email")
    CouponResponse toResponse(Coupon coupon);

    List<CouponResponse> toResponseList(List<Coupon> coupons);

    @Mapping(target = "couponId", source = "coupon.id")
    @Mapping(target = "couponCode", source = "coupon.code")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "orderId", source = "order.id")
    CouponUsageResponse toUsageResponse(CouponUsage usage);

    List<CouponUsageResponse> toUsageResponseList(List<CouponUsage> usages);
}
