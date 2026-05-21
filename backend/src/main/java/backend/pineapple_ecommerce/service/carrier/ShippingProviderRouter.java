package backend.pineapple_ecommerce.service.carrier;

import backend.pineapple_ecommerce.enums.CarrierCode;
import backend.pineapple_ecommerce.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Router điều hướng các yêu cầu đến đúng {@link ShippingCarrierClient}.
 *
 * <p>Sử dụng Spring DI để tự động thu thập tất cả bean implement
 * {@link ShippingCarrierClient}. Thêm carrier mới chỉ cần tạo class
 * với {@code @Component} — không cần sửa router này.
 *
 * <p>Ví dụ dùng:
 * <pre>
 *   ShippingCarrierClient ghn  = router.getClient(CarrierCode.GHN);
 *   ShippingCarrierClient ghtk = router.getClient(CarrierCode.GHTK);
 * </pre>
 */
@Slf4j
@Component
public class ShippingProviderRouter {

    private final Map<CarrierCode, ShippingCarrierClient> clients = new EnumMap<>(CarrierCode.class);

    /**
     * Spring inject tất cả bean implement ShippingCarrierClient.
     * Mỗi bean tự khai báo carrier code của mình qua {@code getCarrierCode()}.
     */
    public ShippingProviderRouter(List<ShippingCarrierClient> carrierClients) {
        for (ShippingCarrierClient client : carrierClients) {
            clients.put(client.getCarrierCode(), client);
        }
    }

    @PostConstruct
    public void logRegisteredCarriers() {
        log.info("Registered shipping carriers: {}", clients.keySet());
    }

    /**
     * Lấy carrier client theo code.
     *
     * @throws BusinessException nếu carrier chưa được hỗ trợ
     */
    public ShippingCarrierClient getClient(CarrierCode carrierCode) {
        ShippingCarrierClient client = clients.get(carrierCode);
        if (client == null) {
            throw new BusinessException(
                    "Đơn vị vận chuyển '" + carrierCode.getDisplayName() + "' chưa được hỗ trợ. " +
                            "Các carrier hiện có: " + clients.keySet()
            );
        }
        return client;
    }

    /** Kiểm tra carrier có được hỗ trợ không. */
    public boolean isSupported(CarrierCode carrierCode) {
        return clients.containsKey(carrierCode);
    }

    /** Danh sách tất cả carrier đang được hỗ trợ. */
    public List<CarrierCode> getSupportedCarriers() {
        return List.copyOf(clients.keySet());
    }
}