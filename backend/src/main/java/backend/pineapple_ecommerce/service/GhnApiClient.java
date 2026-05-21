package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.config.GhnProperties;
import backend.pineapple_ecommerce.dto.ghn.GhnApiDto;
import backend.pineapple_ecommerce.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Low-level HTTP client gọi GHN API.
 *
 * <p>Tách biệt với business logic để dễ mock trong unit test.
 * Tất cả method đều throw BusinessException nếu GHN trả lỗi.
 *
 * <p>Header chuẩn cho GHN:
 * - Token: {api_token}
 * - ShopId: {shop_id}   ← chỉ cần cho một số API
 * - Content-Type: application/json
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GhnApiClient {

    private final GhnProperties ghnProperties;
    private final ObjectMapper objectMapper;

    @Qualifier("ghnRestTemplate")
    private final RestTemplate restTemplate;

    // ─────────────────────────────────────────────
    // Headers helper
    // ─────────────────────────────────────────────

    private HttpHeaders buildHeaders(boolean includeShopId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnProperties.getToken());
        if (includeShopId && ghnProperties.getShopId() != null) {
            headers.set("ShopId", String.valueOf(ghnProperties.getShopId()));
        }
        return headers;
    }

    // ─────────────────────────────────────────────
    // 1. Calculate Fee
    // ─────────────────────────────────────────────

    /**
     * Tính phí giao hàng GHN.
     *
     * @param toDistrictId  District ID người nhận
     * @param toWardCode    Ward code người nhận
     * @param weight        Khối lượng (gram)
     * @param insuranceValue Giá trị bảo hiểm (VNĐ)
     * @param serviceTypeId  Loại dịch vụ (2 = E-commerce)
     * @return FeeData từ GHN
     */
    public GhnApiDto.FeeData calculateFee(
            Integer toDistrictId,
            String toWardCode,
            Integer weight,
            Integer length,
            Integer width,
            Integer height,
            Integer insuranceValue,
            Integer serviceTypeId,
            String coupon) {

        String url = ghnProperties.getBaseUrl() + "/v2/shipping-order/fee";

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("to_district_id", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("weight", weight);
        body.put("length", length);
        body.put("width", width);
        body.put("height", height);
        body.put("insurance_value", insuranceValue);
        body.put("service_type_id", serviceTypeId);
        if (coupon != null) body.put("coupon", coupon);

        GhnApiDto.GhnResponse<GhnApiDto.FeeData> response = post(
                url, body, true,
                new TypeReference<GhnApiDto.GhnResponse<GhnApiDto.FeeData>>() {});

        return response.getData();
    }

    // ─────────────────────────────────────────────
    // 2. Get Services (để lấy service_id)
    // ─────────────────────────────────────────────

    public List<GhnApiDto.ServiceItem> getServices(Integer fromDistrictId, Integer toDistrictId) {
        String url = ghnProperties.getBaseUrl() + "/v2/shipping-order/available-services";

        Map<String, Object> body = Map.of(
                "shop_id", ghnProperties.getShopId(),
                "from_district", fromDistrictId,
                "to_district", toDistrictId
        );

        GhnApiDto.GhnResponse<List<GhnApiDto.ServiceItem>> response = post(
                url, body, false,
                new TypeReference<GhnApiDto.GhnResponse<List<GhnApiDto.ServiceItem>>>() {});

        return response.getData();
    }

    // ─────────────────────────────────────────────
    // 3. Create Order
    // ─────────────────────────────────────────────

    public GhnApiDto.CreateOrderData createOrder(GhnApiDto.CreateOrderRequest request) {
        String url = ghnProperties.getBaseUrl() + "/v2/shipping-order/create";

        GhnApiDto.GhnResponse<GhnApiDto.CreateOrderData> response = post(
                url, request, true,
                new TypeReference<GhnApiDto.GhnResponse<GhnApiDto.CreateOrderData>>() {});

        return response.getData();
    }

    // ─────────────────────────────────────────────
    // 4. Order Info (Tracking)
    // ─────────────────────────────────────────────

    public GhnApiDto.OrderInfoData getOrderInfo(String ghnOrderCode) {
        String url = ghnProperties.getBaseUrl() + "/v2/shipping-order/detail";

        Map<String, Object> body = Map.of("order_code", ghnOrderCode);

        // GHN trả data là array khi success
        GhnApiDto.GhnResponse<List<GhnApiDto.OrderInfoData>> response = post(
                url, body, false,
                new TypeReference<GhnApiDto.GhnResponse<List<GhnApiDto.OrderInfoData>>>() {});

        List<GhnApiDto.OrderInfoData> dataList = response.getData();
        if (dataList == null || dataList.isEmpty()) {
            throw new BusinessException("Không tìm thấy thông tin vận đơn: " + ghnOrderCode);
        }
        return dataList.get(0);
    }

    // ─────────────────────────────────────────────
    // 5. Cancel Order
    // ─────────────────────────────────────────────

    public void cancelOrder(String ghnOrderCode) {
        String url = ghnProperties.getBaseUrl() + "/v2/switch-status/cancel";

        Map<String, Object> body = Map.of(
                "order_codes", List.of(ghnOrderCode)
        );

        post(url, body, false,
                new TypeReference<GhnApiDto.GhnResponse<Object>>() {});

        log.info("GHN order cancelled: {}", ghnOrderCode);
    }

    // ─────────────────────────────────────────────
    // 6. Address APIs
    // ─────────────────────────────────────────────

    public List<GhnApiDto.Province> getProvinces() {
        String url = ghnProperties.getBaseUrl() + "/master-data/province";
        return get(url, false,
                new TypeReference<GhnApiDto.GhnResponse<List<GhnApiDto.Province>>>() {}).getData();
    }

    public List<GhnApiDto.District> getDistricts(Integer provinceId) {
        String url = ghnProperties.getBaseUrl() + "/master-data/district?province_id=" + provinceId;
        return get(url, false,
                new TypeReference<GhnApiDto.GhnResponse<List<GhnApiDto.District>>>() {}).getData();
    }

    public List<GhnApiDto.Ward> getWards(Integer districtId) {
        String url = ghnProperties.getBaseUrl() + "/master-data/ward?district_id=" + districtId;
        return get(url, false,
                new TypeReference<GhnApiDto.GhnResponse<List<GhnApiDto.Ward>>>() {}).getData();
    }

    // ─────────────────────────────────────────────
    // HTTP helpers
    // ─────────────────────────────────────────────

    private <T> T post(String url, Object body, boolean includeShopId, TypeReference<T> typeRef) {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(body, buildHeaders(includeShopId));
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            T parsed = objectMapper.readValue(rawResponse.getBody(), typeRef);
            validateResponse(parsed, url);
            return parsed;

        } catch (HttpClientErrorException e) {
            log.error("GHN API error [POST {}]: status={}, body={}",
                    url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("GHN API lỗi: " + e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GHN API exception [POST {}]: {}", url, e.getMessage(), e);
            throw new BusinessException("Không thể kết nối GHN API: " + e.getMessage());
        }
    }

    private <T> T get(String url, boolean includeShopId, TypeReference<T> typeRef) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(includeShopId));
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            T parsed = objectMapper.readValue(rawResponse.getBody(), typeRef);
            validateResponse(parsed, url);
            return parsed;

        } catch (HttpClientErrorException e) {
            log.error("GHN API error [GET {}]: status={}, body={}",
                    url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("GHN API lỗi: " + e.getMessage());
        } catch (Exception e) {
            log.error("GHN API exception [GET {}]: {}", url, e.getMessage(), e);
            throw new BusinessException("Không thể kết nối GHN API: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void validateResponse(T response, String url) {
        if (response instanceof GhnApiDto.GhnResponse<?> ghnResp) {
            if (!ghnResp.isSuccess()) {
                log.warn("GHN API returned non-200 [{}]: code={}, message={}",
                        url, ghnResp.getCode(), ghnResp.getMessage());
                throw new BusinessException("GHN: " + ghnResp.getMessage());
            }
        }
    }
}
