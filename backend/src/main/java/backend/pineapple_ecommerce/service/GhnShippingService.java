package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.ghn.GhnApiDto;
import backend.pineapple_ecommerce.dto.request.CalculateShippingFeeRequest;
import backend.pineapple_ecommerce.dto.response.ShippingFeeResponse;
import backend.pineapple_ecommerce.dto.response.ShippingTrackingResponse;

import java.util.List;

/**
 * Service xử lý nghiệp vụ giao hàng GHN.
 *
 * <p>Các chức năng:
 * 1. calculateFee      — Tính phí trước khi checkout
 * 2. createShipment    — Tạo vận đơn GHN sau khi order được PROCESSING
 * 3. getTracking       — Lấy trạng thái vận đơn (user xem)
 * 4. syncStatus        — Đồng bộ trạng thái từ GHN về DB (gọi bởi webhook/scheduler)
 * 5. cancelShipment    — Hủy vận đơn GHN khi order bị hủy
 * 6. getProvinces/Districts/Wards — Dữ liệu địa chỉ GHN
 */
public interface GhnShippingService {

    /**
     * Tính phí giao hàng.
     * Gọi khi user ở bước checkout để hiển thị phí trước khi đặt hàng.
     */
    ShippingFeeResponse calculateFee(CalculateShippingFeeRequest request);

    /**
     * Tạo vận đơn GHN cho đơn hàng.
     * Gọi khi Admin chuyển trạng thái từ CONFIRMED → PROCESSING.
     *
     * @param orderId  ID đơn hàng nội bộ
     * @return Thông tin vận đơn đã tạo (ghn_order_code, total_fee, ...)
     */
    ShippingTrackingResponse createShipment(Long orderId);

    /**
     * Lấy thông tin tracking vận đơn cho user.
     * Trả về trạng thái hiện tại + lịch sử từ DB (không gọi GHN trực tiếp — dùng cache DB).
     *
     * @param orderId  ID đơn hàng của user hiện tại
     * @param userId   Dùng để authorize (chỉ xem đơn của mình)
     */
    ShippingTrackingResponse getTracking(Long orderId, Long userId);

    /**
     * Đồng bộ trạng thái từ GHN về DB.
     * Gọi từ Webhook handler hoặc scheduled job.
     *
     * @param ghnOrderCode  Mã vận đơn GHN
     */
    void syncStatusFromGhn(String ghnOrderCode);

    /**
     * Hủy vận đơn GHN.
     * Gọi khi order bị CANCELLED (chỉ được hủy khi vận đơn đang ở trạng thái ready_to_pick/picking).
     *
     * @param orderId  ID đơn hàng nội bộ
     */
    void cancelShipment(Long orderId);

    // ── Address master data ──────────────────────
    List<GhnApiDto.Province>  getProvinces();
    List<GhnApiDto.District>  getDistricts(Integer provinceId);
    List<GhnApiDto.Ward>      getWards(Integer districtId);
}
