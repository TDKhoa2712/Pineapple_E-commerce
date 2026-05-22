package backend.pineapple_ecommerce.infrastructure.carrier;

import backend.pineapple_ecommerce.common.enums.CarrierCode;
import backend.pineapple_ecommerce.common.enums.ShippingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component; // Bỏ comment khi sẵn sàng activate

import java.util.List;

/**
 * Skeleton implementation GHTK — ví dụ về cách thêm carrier mới.
 *
 * <p><b>Hướng dẫn activate:</b>
 * <ol>
 *   <li>Bỏ comment {@code @Component} ở trên class
 *   <li>Tạo {@code GhtkProperties} với {@code @ConfigurationProperties(prefix = "app.ghtk")}
 *   <li>Thêm config vào application.yml: {@code app.ghtk.token}, {@code app.ghtk.base-url}
 *   <li>Implement các method dựa theo GHTK API docs
 *   <li>Class sẽ tự động được đăng ký vào {@link ShippingProviderRouter}
 * </ol>
 *
 * <p>Không cần sửa bất kỳ file nào khác.
 *
 * <p>GHTK API docs: https://docs.giaohangtietkiem.vn/
 */
@Slf4j
@RequiredArgsConstructor
// @Component  ← Bỏ comment khi implement xong
public class GhtkCarrierClient implements ShippingCarrierClient {

    // private final GhtkProperties ghtkProperties;
    // private final RestTemplate restTemplate;
    // private final ObjectMapper objectMapper;

    @Override
    public CarrierCode getCarrierCode() {
        return CarrierCode.GHTK;
    }

    @Override
    public FeeResult calculateFee(FeeRequest request) {
        // TODO: POST https://services.giaohangtietkiem.vn/services/shipment/fee
        // Headers: Token: {token}, X-Client-Source: {partner_id}
        // Body: { "pick_province": ..., "pick_district": ..., "province": ...,
        //         "district": ..., "address": ..., "weight": ..., "transport": "road" }
        throw new UnsupportedOperationException("GHTK calculateFee chưa được implement");
    }

    @Override
    public CreateShipmentResult createShipment(CreateShipmentRequest request) {
        // TODO: POST https://services.giaohangtietkiem.vn/services/shipment/order
        // GHTK trả về: { "order": { "label": "S12345678.1.2", "fee": 25000 } }
        throw new UnsupportedOperationException("GHTK createShipment chưa được implement");
    }

    @Override
    public TrackingResult getTracking(String externalOrderCode) {
        // TODO: GET https://services.giaohangtietkiem.vn/services/shipment/v2/{order_id}
        // GHTK status codes: -1=Hủy, 1=Chưa tiếp nhận, 2=Đã tiếp nhận, ...
        throw new UnsupportedOperationException("GHTK getTracking chưa được implement");
    }

    @Override
    public void cancelShipment(String externalOrderCode) {
        // TODO: POST https://services.giaohangtietkiem.vn/services/shipment/cancel/{order_id}
        throw new UnsupportedOperationException("GHTK cancelShipment chưa được implement");
    }

    @Override
    public List<LocationItem> getProvinces() {
        // GHTK không có Address API riêng — dùng danh sách tỉnh chuẩn Việt Nam từ DB nội bộ
        // hoặc hardcode list 63 tỉnh thành
        throw new UnsupportedOperationException("GHTK getProvinces chưa được implement");
    }

    @Override
    public List<LocationItem> getDistricts(String provinceId) {
        throw new UnsupportedOperationException("GHTK getDistricts chưa được implement");
    }

    @Override
    public List<LocationItem> getWards(String districtId) {
        throw new UnsupportedOperationException("GHTK getWards chưa được implement");
    }

    // ── GHTK Status Mapping ──────────────────────────────────────────

    /**
     * Map status code GHTK → ShippingStatus chuẩn.
     * Ref: https://docs.giaohangtietkiem.vn/?shell#tr-ng-th-i-n-h-ng
     */
    private static ShippingStatus normalizeGhtkStatus(int ghtkStatusCode) {
        return switch (ghtkStatusCode) {
            case -1         -> ShippingStatus.CANCELLED;
            case 1          -> ShippingStatus.PENDING_PICKUP;    // Chưa tiếp nhận
            case 2          -> ShippingStatus.PENDING_PICKUP;    // Đã tiếp nhận
            case 3          -> ShippingStatus.PICKING_UP;        // Đã lấy hàng / nhập kho
            case 4          -> ShippingStatus.IN_TRANSIT;        // Đã điều phối giao
            case 5          -> ShippingStatus.OUT_FOR_DELIVERY;  // Đang giao
            case 6          -> ShippingStatus.DELIVERED;         // Đã giao
            case 7, 10      -> ShippingStatus.DELIVERY_FAILED;   // Giao thất bại
            case 8          -> ShippingStatus.RETURNING;         // Đang hoàn
            case 9          -> ShippingStatus.RETURNED;          // Đã hoàn
            case 20         -> ShippingStatus.EXCEPTION;         // Đang hoàn — chờ xử lý
            default         -> ShippingStatus.UNKNOWN;
        };
    }
}