package backend.pineapple_ecommerce.service.carrier;

import backend.pineapple_ecommerce.enums.ShippingStatus;

import java.util.Map;

/**
 * Map trạng thái gốc GHN → {@link ShippingStatus} chuẩn nội bộ.
 *
 * <p>Tách biệt khỏi carrier client để dễ test độc lập.
 * Ref: https://api.ghn.vn/home/docs/detail?id=48
 */
public final class GhnStatusMapper {

    private GhnStatusMapper() {}

    private static final Map<String, ShippingStatus> MAPPING = Map.ofEntries(
            Map.entry("ready_to_pick",             ShippingStatus.PENDING_PICKUP),
            Map.entry("picking",                   ShippingStatus.PICKING_UP),
            Map.entry("picked",                    ShippingStatus.PICKED_UP),
            Map.entry("money_collect_picking",     ShippingStatus.PICKING_UP),
            Map.entry("storing",                   ShippingStatus.AT_WAREHOUSE),
            Map.entry("transporting",              ShippingStatus.IN_TRANSIT),
            Map.entry("sorting",                   ShippingStatus.SORTING),
            Map.entry("delivering",                ShippingStatus.OUT_FOR_DELIVERY),
            Map.entry("money_collect_delivering",  ShippingStatus.OUT_FOR_DELIVERY),
            Map.entry("delivered",                 ShippingStatus.DELIVERED),
            Map.entry("delivery_fail",             ShippingStatus.DELIVERY_FAILED),
            Map.entry("waiting_to_return",         ShippingStatus.RETURNING),
            Map.entry("return",                    ShippingStatus.RETURNING),
            Map.entry("returned",                  ShippingStatus.RETURNED),
            Map.entry("exception",                 ShippingStatus.EXCEPTION),
            Map.entry("damage",                    ShippingStatus.DAMAGED),
            Map.entry("lost",                      ShippingStatus.LOST),
            Map.entry("cancel",                    ShippingStatus.CANCELLED)
    );

    public static ShippingStatus normalize(String ghnStatus) {
        if (ghnStatus == null) return ShippingStatus.UNKNOWN;
        return MAPPING.getOrDefault(ghnStatus.toLowerCase(), ShippingStatus.UNKNOWN);
    }
}