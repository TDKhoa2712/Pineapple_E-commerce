package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.CalculateShippingFeeRequest;
import backend.pineapple_ecommerce.dto.response.ShippingFeeResponse;
import backend.pineapple_ecommerce.dto.response.ShippingTrackingResponse;
import backend.pineapple_ecommerce.enums.CarrierCode;
import backend.pineapple_ecommerce.service.carrier.ShippingCarrierClient;

import java.util.List;

/**
 * Service giao hàng không phụ thuộc vào bất kỳ carrier cụ thể nào.
 *
 * <p>Controller chỉ biết interface này. Carrier được chọn thông qua
 * tham số {@link CarrierCode} hoặc cấu hình mặc định từ application.yml.
 *
 * <p>Khi thêm carrier mới, interface này KHÔNG cần thay đổi.
 */
public interface ShippingService {

    /**
     * Tính phí giao hàng.
     *
     * @param request      Thông tin tính phí
     * @param carrierCode  Carrier muốn dùng (null = dùng carrier mặc định)
     */
    ShippingFeeResponse calculateFee(CalculateShippingFeeRequest request, CarrierCode carrierCode);

    /**
     * Tạo vận đơn cho đơn hàng đã được xác nhận.
     *
     * @param orderId      ID đơn hàng nội bộ
     * @param carrierCode  Carrier muốn dùng (null = dùng carrier mặc định)
     */
    ShippingTrackingResponse createShipment(Long orderId, CarrierCode carrierCode);

    /**
     * Lấy thông tin tracking vận đơn (đọc từ DB cache, không gọi carrier trực tiếp).
     *
     * @param orderId  ID đơn hàng
     * @param userId   Dùng để authorize (chỉ xem đơn của mình; null = admin bypass)
     */
    ShippingTrackingResponse getTracking(Long orderId, Long userId);

    /**
     * Đồng bộ trạng thái vận đơn từ carrier về DB.
     * Gọi từ webhook handler hoặc scheduler.
     *
     * @param externalOrderCode  Mã vận đơn do carrier cấp
     * @param carrierCode        Carrier tương ứng
     */
    void syncStatus(String externalOrderCode, CarrierCode carrierCode);

    /**
     * Hủy vận đơn.
     *
     * @param orderId  ID đơn hàng nội bộ
     */
    void cancelShipment(Long orderId);

    // ── Address master data ──────────────────────────────────────────

    List<ShippingCarrierClient.LocationItem> getProvinces(CarrierCode carrierCode);

    List<ShippingCarrierClient.LocationItem> getDistricts(CarrierCode carrierCode, String provinceId);

    List<ShippingCarrierClient.LocationItem> getWards(CarrierCode carrierCode, String districtId);

    /** Danh sách các carrier đang được hỗ trợ */
    List<CarrierCode> getSupportedCarriers();
}