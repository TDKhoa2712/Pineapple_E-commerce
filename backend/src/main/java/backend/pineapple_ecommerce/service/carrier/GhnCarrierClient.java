package backend.pineapple_ecommerce.service.carrier;

import backend.pineapple_ecommerce.config.GhnProperties;
import backend.pineapple_ecommerce.enums.CarrierCode;
import backend.pineapple_ecommerce.enums.ShippingStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Implementation GHN của {@link ShippingCarrierClient}.
 *
 * <p>Class này đóng gói toàn bộ chi tiết HTTP / response format của GHN.
 * Phần còn lại của hệ thống chỉ biết đến interface {@link ShippingCarrierClient},
 * hoàn toàn không biết GHN cụ thể dùng endpoint nào hay JSON ra sao.
 *
 * <p>Khi GHN thay đổi API version (v2 → v3), chỉ cần sửa file này.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GhnCarrierClient implements ShippingCarrierClient {

    private final GhnProperties ghnProperties;
    private final ObjectMapper objectMapper;

    @Qualifier("ghnRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public CarrierCode getCarrierCode() {
        return CarrierCode.GHN;
    }

    // ─────────────────────────────────────────────
    // Calculate Fee
    // ─────────────────────────────────────────────

    @Override
    public FeeResult calculateFee(FeeRequest req) {
        String url = ghnProperties.getBaseUrl() + "/v2/shipping-order/fee";

        Map<String, Object> body = new HashMap<>();
        body.put("to_district_id", Integer.parseInt(req.toDistrictId()));
        body.put("to_ward_code", req.toWardCode());
        body.put("weight", req.weightGram());
        body.put("length", req.lengthCm());
        body.put("width", req.widthCm());
        body.put("height", req.heightCm());
        body.put("insurance_value", req.insuranceValue());
        body.put("service_type_id", parseServiceType(req.serviceType(), 2));
        if (req.couponCode() != null) body.put("coupon", req.couponCode());

        Map<String, Object> data = postAndExtractData(url, body, true);

        return new FeeResult(
                toBigDecimal(data, "service_fee"),
                toBigDecimal(data, "insurance_fee"),
                toBigDecimal(data, "cod_fee"),
                toBigDecimal(data, "coupon_value"),
                toBigDecimal(data, "total"),
                null,
                null
        );
    }

    // ─────────────────────────────────────────────
    // Create Shipment
    // ─────────────────────────────────────────────

    @Override
    public CreateShipmentResult createShipment(CreateShipmentRequest req) {
        String url = ghnProperties.getBaseUrl() + "/v2/shipping-order/create";

        Map<String, Object> body = new HashMap<>();
        body.put("payment_type_id", req.isCod() ? 2 : 1);
        body.put("required_note", "KHONGCHOXEMHANG");
        body.put("to_name", req.toName());
        body.put("to_phone", req.toPhone());
        body.put("to_address", req.toAddress());
        body.put("to_ward_code", req.toWardCode());
        body.put("to_district_id", Integer.parseInt(req.toDistrictId()));
        body.put("cod_amount", req.isCod() ? req.codAmount() : 0);
        body.put("weight", req.weightGram());
        body.put("length", req.lengthCm());
        body.put("width", req.widthCm());
        body.put("height", req.heightCm());
        body.put("insurance_value", req.insuranceValue());
        body.put("service_type_id", parseServiceType(req.serviceType(), 2));
        body.put("client_order_code", req.clientOrderCode());
        if (req.note() != null) body.put("note", req.note());

        List<Map<String, Object>> items = new ArrayList<>();
        for (ShipmentItem item : req.items()) {
            Map<String, Object> i = new HashMap<>();
            i.put("name", item.name());
            i.put("quantity", item.quantity());
            i.put("weight", item.weightGram());
            i.put("price", item.price());
            items.add(i);
        }
        body.put("items", items);

        Map<String, Object> data = postAndExtractData(url, body, true);

        String orderCode       = (String) data.get("order_code");
        String sortCode        = (String) data.get("sort_code");
        String totalFeeStr     = Objects.toString(data.get("total_fee"), "0");
        String expectedTime    = (String) data.get("expected_delivery_time");

        // Lưu GHN-specific metadata
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("sortCode", sortCode);
        meta.put("shopId", ghnProperties.getShopId());
        String metaJson;
        try {
            metaJson = objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            metaJson = "{}";
        }

        return new CreateShipmentResult(
                orderCode,
                sortCode,
                parseFee(totalFeeStr),
                parseFee(totalFeeStr),
                expectedTime,
                metaJson
        );
    }

    // ─────────────────────────────────────────────
    // Tracking
    // ─────────────────────────────────────────────

    @Override
    public TrackingResult getTracking(String externalOrderCode) {
        String url = ghnProperties.getBaseUrl() + "/v2/shipping-order/detail";
        Map<String, Object> body = Map.of("order_code", externalOrderCode);

        // SỬA TẠI ĐÂY: Sử dụng hàm trả về Map (Ví dụ: postAndExtractData) thay vì postAndExtractDataList
        Map<String, Object> data = postAndExtractData(url, body, false);

        if (data == null) {
            throw new BusinessException("Không tìm thấy vận đơn GHN: " + externalOrderCode);
        }

        // Lấy dữ liệu trực tiếp từ Map thay vì gọi dataList.get(0)
        String rawStatus = (String) data.get("status");
        ShippingStatus normalized = GhnStatusMapper.normalize(rawStatus);

        List<StatusLogEntry> history = new ArrayList<>();
        Object logObj = data.get("log");
        if (logObj instanceof List<?> logs) {
            for (Object entry : logs) {
                if (entry instanceof Map<?, ?> logEntry) {
                    String s = (String) logEntry.get("status");
                    String d = (String) logEntry.get("updated_date");
                    ShippingStatus ns = GhnStatusMapper.normalize(s);
                    history.add(new StatusLogEntry(ns, s, ns.getDescription(), parseDateTime(d)));
                }
            }
        }

        return new TrackingResult(
                externalOrderCode,
                normalized,
                rawStatus,
                normalized.getDescription(),
                (String) data.get("reason"),
                history
        );
    }

    // ─────────────────────────────────────────────
    // Cancel
    // ─────────────────────────────────────────────

    @Override
    public void cancelShipment(String externalOrderCode) {
        String url = ghnProperties.getBaseUrl() + "/v2/switch-status/cancel";
        Map<String, Object> body = Map.of("order_codes", List.of(externalOrderCode));
        postAndExtractData(url, body, false);
        log.info("GHN shipment cancelled: {}", externalOrderCode);
    }

    // ─────────────────────────────────────────────
    // Address master data
    // ─────────────────────────────────────────────

    @Override
    public List<LocationItem> getProvinces() {
        String url = ghnProperties.getBaseUrl() + "/master-data/province";
        List<Map<String, Object>> list = getAndExtractDataList(url, false);
        return list.stream()
                .map(m -> new LocationItem(
                        Objects.toString(m.get("ProvinceID"), ""),
                        (String) m.get("ProvinceName"),
                        null))
                .toList();
    }

    @Override
    public List<LocationItem> getDistricts(String provinceId) {
        String url = ghnProperties.getBaseUrl() + "/master-data/district?province_id=" + provinceId;
        List<Map<String, Object>> list = getAndExtractDataList(url, false);
        return list.stream()
                .map(m -> new LocationItem(
                        Objects.toString(m.get("DistrictID"), ""),
                        (String) m.get("DistrictName"),
                        Objects.toString(m.get("ProvinceID"), null)))
                .toList();
    }

    @Override
    public List<LocationItem> getWards(String districtId) {
        String url = ghnProperties.getBaseUrl() + "/master-data/ward?district_id=" + districtId;
        List<Map<String, Object>> list = getAndExtractDataList(url, false);
        return list.stream()
                .map(m -> new LocationItem(
                        (String) m.get("WardCode"),
                        (String) m.get("WardName"),
                        Objects.toString(m.get("DistrictID"), null)))
                .toList();
    }

    // ─────────────────────────────────────────────
    // HTTP Helpers
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> postAndExtractData(String url, Object body, boolean includeShopId) {
        String raw = executePost(url, body, includeShopId);
        Map<String, Object> response = parseJson(raw, new TypeReference<Map<String, Object>>() {});
        validateGhnResponse(response, url);
        return (Map<String, Object>) response.get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> postAndExtractDataList(String url, Object body, boolean includeShopId) {
        String raw = executePost(url, body, includeShopId);
        Map<String, Object> response = parseJson(raw, new TypeReference<Map<String, Object>>() {});
        validateGhnResponse(response, url);
        return (List<Map<String, Object>>) response.get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAndExtractDataList(String url, boolean includeShopId) {
        String raw = executeGet(url, includeShopId);
        Map<String, Object> response = parseJson(raw, new TypeReference<Map<String, Object>>() {});
        validateGhnResponse(response, url);
        Object data = response.get("data");
        return data instanceof List ? (List<Map<String, Object>>) data : List.of();
    }

    private String executePost(String url, Object body, boolean includeShopId) {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(body, buildHeaders(includeShopId));
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return resp.getBody();
        } catch (HttpClientErrorException e) {
            log.error("GHN POST error [{}]: status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("GHN API lỗi: " + e.getMessage());
        } catch (Exception e) {
            log.error("GHN POST exception [{}]: {}", url, e.getMessage(), e);
            throw new BusinessException("Không thể kết nối GHN API: " + e.getMessage());
        }
    }

    private String executeGet(String url, boolean includeShopId) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(includeShopId));
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("GHN GET exception [{}]: {}", url, e.getMessage(), e);
            throw new BusinessException("Không thể kết nối GHN API: " + e.getMessage());
        }
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new BusinessException("Lỗi parse response GHN: " + e.getMessage());
        }
    }

    private void validateGhnResponse(Map<String, Object> response, String url) {
        Object code = response.get("code");
        if (!Integer.valueOf(200).equals(code)) {
            String message = (String) response.getOrDefault("message", "Unknown error");
            log.warn("GHN non-200 [{}]: code={}, message={}", url, code, message);
            throw new BusinessException("GHN: " + message);
        }
    }

    private BigDecimal toBigDecimal(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val == null) return BigDecimal.ZERO;
        try { return new BigDecimal(val.toString()); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private BigDecimal parseFee(String value) {
        if (value == null) return BigDecimal.ZERO;
        try { return new BigDecimal(value.replaceAll("[^0-9.]", "")); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private LocalDateTime parseDateTime(String iso) {
        if (iso == null || iso.isBlank() || "null".equals(iso)) return null;
        try { return OffsetDateTime.parse(iso).toLocalDateTime(); }
        catch (Exception e) {
            try { return LocalDateTime.parse(iso); }
            catch (Exception ex) { return null; }
        }
    }

    private int parseServiceType(String serviceType, int defaultVal) {
        if (serviceType == null) return defaultVal;
        try { return Integer.parseInt(serviceType); }
        catch (Exception e) { return defaultVal; }
    }
}