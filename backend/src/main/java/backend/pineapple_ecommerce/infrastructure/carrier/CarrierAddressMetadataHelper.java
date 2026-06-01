package backend.pineapple_ecommerce.infrastructure.carrier;

import backend.pineapple_ecommerce.common.enums.CarrierCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper đọc/ghi metadata địa chỉ carrier-specific từ trường {@code carrierMetadata} (JSON).
 *
 * <p>Cấu trúc JSON trong {@code Address.carrierMetadata}:
 * <pre>
 * {
 *   "GHN":  { "districtId": "1454", "wardCode": "21307" },
 *   "GHTK": { "province": "Hồ Chí Minh", "district": "Quận 1" }
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarrierAddressMetadataHelper {

    private final ObjectMapper objectMapper;

    /**
     * Đọc metadata của một carrier cụ thể từ JSON.
     *
     * @return Map metadata, hoặc empty map nếu chưa có
     */
    public Map<String, String> getMetadata(String carrierMetadataJson, CarrierCode carrierCode) {
        if (carrierMetadataJson == null || carrierMetadataJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            Map<String, Map<String, String>> all = objectMapper.readValue(
                    carrierMetadataJson,
                    new TypeReference<Map<String, Map<String, String>>>() {}
            );
            return all.getOrDefault(carrierCode.name(), new HashMap<>());
        } catch (Exception e) {
            log.warn("Failed to parse carrierMetadata for {}: {}", carrierCode, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Ghi metadata của một carrier vào JSON, giữ nguyên metadata của các carrier khác.
     *
     * @param existingJson JSON hiện tại (có thể null)
     * @param carrierCode  Carrier cần cập nhật
     * @param metadata     Dữ liệu mới
     * @return JSON đã cập nhật
     */
    public String setMetadata(String existingJson, CarrierCode carrierCode, Map<String, String> metadata) {
        try {
            Map<String, Map<String, String>> all = new HashMap<>();
            if (existingJson != null && !existingJson.isBlank()) {
                all = objectMapper.readValue(existingJson,
                        new TypeReference<Map<String, Map<String, String>>>() {});
            }
            all.put(carrierCode.name(), metadata);
            return objectMapper.writeValueAsString(all);
        } catch (Exception e) {
            log.error("Failed to set carrierMetadata for {}: {}", carrierCode, e.getMessage());
            return existingJson;
        }
    }

    // ── Helpers tiện ích cho GHN ──────────────────────────────────────

    /** Lấy GHN districtId từ address metadata. */
    public String getGhnDistrictId(String carrierMetadataJson) {
        return getMetadata(carrierMetadataJson, CarrierCode.GHN).get("districtId");
    }

    /** Lấy GHN wardCode từ address metadata. */
    public String getGhnWardCode(String carrierMetadataJson) {
        return getMetadata(carrierMetadataJson, CarrierCode.GHN).get("wardCode");
    }

    /** Lưu GHN districtId và wardCode vào address metadata. */
    public String saveGhnMetadata(String existingJson, String districtId, String wardCode) {
        Map<String, String> ghnMeta = new HashMap<>();
        ghnMeta.put("districtId", districtId);
        ghnMeta.put("wardCode", wardCode);
        return setMetadata(existingJson, CarrierCode.GHN, ghnMeta);
    }
}