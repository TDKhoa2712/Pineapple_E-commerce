package backend.pineapple_ecommerce.service.carrier;

import backend.pineapple_ecommerce.enums.CarrierCode;
import backend.pineapple_ecommerce.enums.ShippingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface cốt lõi mỗi đơn vị vận chuyển phải implement.
 *
 * <p><b>Để thêm carrier mới (ví dụ GHTK):</b>
 * <ol>
 *   <li>Tạo class {@code GhtkCarrierClient implements ShippingCarrierClient}
 *   <li>Implement các method theo API của GHTK
 *   <li>Tạo {@code GhtkProperties} với {@code @ConfigurationProperties(prefix = "app.ghtk")}
 *   <li>Thêm {@code GHTK} vào enum {@link CarrierCode}
 *   <li>Class mới tự động được đăng ký nhờ {@code @Component} và inject vào {@link ShippingProviderRouter}
 * </ol>
 *
 * <p>Không cần sửa ShippingController, ShippingService, hay bất kỳ code nào khác.
 */
public interface ShippingCarrierClient {

    /**
     * Carrier code mà client này xử lý.
     * Dùng để router điều hướng đúng carrier.
     */
    CarrierCode getCarrierCode();

    /**
     * Tính phí giao hàng ước tính.
     *
     * @param request thông tin cần thiết để tính phí (destination, weight, dimensions, ...)
     * @return kết quả tính phí
     */
    FeeResult calculateFee(FeeRequest request);

    /**
     * Tạo vận đơn trên hệ thống carrier.
     *
     * @param request thông tin đơn hàng cần giao
     * @return kết quả tạo vận đơn (mã vận đơn, phí thực tế, ...)
     */
    CreateShipmentResult createShipment(CreateShipmentRequest request);

    /**
     * Lấy thông tin tracking theo mã vận đơn carrier.
     *
     * @param externalOrderCode mã vận đơn do carrier cấp
     * @return thông tin tracking hiện tại
     */
    TrackingResult getTracking(String externalOrderCode);

    /**
     * Hủy vận đơn trên hệ thống carrier.
     *
     * @param externalOrderCode mã vận đơn cần hủy
     * @throws backend.pineapple_ecommerce.exception.BusinessException nếu không thể hủy
     */
    void cancelShipment(String externalOrderCode);

    /**
     * Lấy danh sách tỉnh/thành (master data địa chỉ).
     * Một số carrier không có API này — trả về danh sách từ DB nội bộ.
     */
    List<LocationItem> getProvinces();

    /**
     * Lấy danh sách quận/huyện theo tỉnh.
     *
     * @param provinceId ID tỉnh theo hệ thống của carrier
     */
    List<LocationItem> getDistricts(String provinceId);

    /**
     * Lấy danh sách phường/xã theo quận.
     *
     * @param districtId ID quận theo hệ thống của carrier
     */
    List<LocationItem> getWards(String districtId);

    // ═════════════════════════════════════════════════════════════════
    // Inner DTOs — dùng giữa Service và CarrierClient
    // (Không expose ra Controller — Controller dùng DTO riêng)
    // ═════════════════════════════════════════════════════════════════

    /** Request tính phí giao hàng */
    record FeeRequest(
            String toProvinceId,
            String toDistrictId,
            String toWardCode,
            int weightGram,
            int lengthCm,
            int widthCm,
            int heightCm,
            int insuranceValue,
            String serviceType,   // Carrier-specific: GHN="2", GHTK="road"
            String couponCode
    ) {}

    /** Kết quả tính phí */
    record FeeResult(
            BigDecimal serviceFee,
            BigDecimal insuranceFee,
            BigDecimal codFee,
            BigDecimal couponDiscount,
            BigDecimal totalFee,
            String expectedDeliveryTime,  // ISO string
            String serviceId              // Service ID cụ thể carrier chọn
    ) {}

    /** Request tạo vận đơn */
    record CreateShipmentRequest(
            // Người nhận
            String toName,
            String toPhone,
            String toAddress,
            String toProvinceId,
            String toDistrictId,
            String toWardCode,

            // Thông tin hàng
            int weightGram,
            int lengthCm,
            int widthCm,
            int heightCm,
            int insuranceValue,
            String serviceType,
            String note,

            // Thanh toán
            boolean isCod,
            int codAmount,

            // Đối soát nội bộ
            String clientOrderCode,

            // Chi tiết sản phẩm (cho một số carrier yêu cầu)
            List<ShipmentItem> items
    ) {}

    /** Một mặt hàng trong vận đơn */
    record ShipmentItem(
            String name,
            int quantity,
            int weightGram,
            int price
    ) {}

    /** Kết quả tạo vận đơn */
    record CreateShipmentResult(
            String externalOrderCode,   // Mã vận đơn carrier cấp
            String sortCode,            // Mã sắp xếp / label (nếu có)
            BigDecimal shippingFee,
            BigDecimal totalFee,
            String expectedDeliveryTime,
            String carrierMetadataJson  // JSON lưu thêm data carrier-specific
    ) {}

    /** Kết quả tracking */
    record TrackingResult(
            String externalOrderCode,
            ShippingStatus normalizedStatus,  // Đã normalize
            String rawStatus,                  // Trạng thái gốc từ carrier
            String rawStatusLabel,
            String failReason,
            List<StatusLogEntry> statusHistory
    ) {}

    /** Một mốc trong lịch sử tracking */
    record StatusLogEntry(
            ShippingStatus normalizedStatus,
            String rawStatus,
            String rawStatusLabel,
            LocalDateTime updatedAt
    ) {}

    /** Item địa chỉ master data (tỉnh / quận / phường) */
    record LocationItem(
            String id,          // ID theo hệ thống carrier
            String name,        // Tên hiển thị
            String parentId     // ID cha (tỉnh của quận, quận của phường)
    ) {}
}