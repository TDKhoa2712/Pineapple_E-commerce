package backend.pineapple_ecommerce.dto.ghn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Các DTO nội bộ để parse response từ GHN API.
 * Không expose ra ngoài — chỉ dùng trong GhnApiClient.
 */
public class GhnApiDto {

    // ─────────────────────────────────────────────
    // Wrapper response chung
    // ─────────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GhnResponse<T> {
        private Integer code;
        private String message;
        private T data;

        public boolean isSuccess() {
            return Integer.valueOf(200).equals(code);
        }
    }

    // ─────────────────────────────────────────────
    // Calculate Fee
    // ─────────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeeData {
        private Integer total;

        @JsonProperty("service_fee")
        private Integer serviceFee;

        @JsonProperty("insurance_fee")
        private Integer insuranceFee;

        @JsonProperty("pick_station_fee")
        private Integer pickStationFee;

        @JsonProperty("coupon_value")
        private Integer couponValue;

        @JsonProperty("r2s_fee")
        private Integer r2sFee;

        @JsonProperty("cod_fee")
        private Integer codFee;

        @JsonProperty("pick_remote_areas_fee")
        private Integer pickRemoteAreasFee;

        @JsonProperty("deliver_remote_areas_fee")
        private Integer deliverRemoteAreasFee;

        @JsonProperty("cod_failed_fee")
        private Integer codFailedFee;
    }

    // ─────────────────────────────────────────────
    // Create Order
    // ─────────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateOrderData {
        @JsonProperty("order_code")
        private String orderCode;

        @JsonProperty("sort_code")
        private String sortCode;

        @JsonProperty("total_fee")
        private String totalFee;

        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime;

        @JsonProperty("trans_type")
        private String transType;

        private FeeData fee;
    }

    // ─────────────────────────────────────────────
    // Order Info (Tracking)
    // ─────────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderInfoData {
        @JsonProperty("order_code")
        private String orderCode;

        private String status;

        @JsonProperty("client_order_code")
        private String clientOrderCode;

        @JsonProperty("to_name")
        private String toName;

        @JsonProperty("to_phone")
        private String toPhone;

        @JsonProperty("to_address")
        private String toAddress;

        private Integer weight;

        @JsonProperty("leadtime")
        private String expectedDeliveryTime;

        @JsonProperty("finish_date")
        private String finishDate;

        private List<StatusLog> log;

        @JsonProperty("pick_warehouse_id")
        private Integer pickWarehouseId;

        @JsonProperty("deliver_warehouse_id")
        private Integer deliverWarehouseId;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusLog {
        private String status;

        @JsonProperty("updated_date")
        private String updatedDate;
    }

    // ─────────────────────────────────────────────
    // Address APIs
    // ─────────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Province {
        @JsonProperty("ProvinceID")
        private Integer provinceId;

        @JsonProperty("ProvinceName")
        private String provinceName;

        @JsonProperty("Code")
        private String code;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class District {
        @JsonProperty("DistrictID")
        private Integer districtId;

        @JsonProperty("DistrictName")
        private String districtName;

        @JsonProperty("ProvinceID")
        private Integer provinceId;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ward {
        @JsonProperty("WardCode")
        private String wardCode;

        @JsonProperty("WardName")
        private String wardName;

        @JsonProperty("DistrictID")
        private Integer districtId;
    }

    // ─────────────────────────────────────────────
    // Get Service
    // ─────────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceItem {
        @JsonProperty("service_id")
        private Integer serviceId;

        @JsonProperty("short_name")
        private String shortName;

        @JsonProperty("service_type_id")
        private Integer serviceTypeId;
    }

    // ─────────────────────────────────────────────
    // Create Order Request (gửi lên GHN)
    // ─────────────────────────────────────────────

    @Getter
    @Setter
    public static class CreateOrderRequest {
        @JsonProperty("payment_type_id")
        private Integer paymentTypeId = 1; // 1 = Shop trả, 2 = Người nhận trả

        private String note;

        @JsonProperty("required_note")
        private String requiredNote = "KHONGCHOXEMHANG";

        @JsonProperty("from_name")
        private String fromName;

        @JsonProperty("from_phone")
        private String fromPhone;

        @JsonProperty("from_address")
        private String fromAddress;

        @JsonProperty("from_ward_name")
        private String fromWardName;

        @JsonProperty("from_district_name")
        private String fromDistrictName;

        @JsonProperty("from_province_name")
        private String fromProvinceName;

        @JsonProperty("to_name")
        private String toName;

        @JsonProperty("to_phone")
        private String toPhone;

        @JsonProperty("to_address")
        private String toAddress;

        @JsonProperty("to_ward_code")
        private String toWardCode;

        @JsonProperty("to_district_id")
        private Integer toDistrictId;

        @JsonProperty("cod_amount")
        private Integer codAmount = 0;

        private String content;

        private Integer weight;
        private Integer length = 20;
        private Integer width = 20;
        private Integer height = 10;

        @JsonProperty("insurance_value")
        private Integer insuranceValue = 0;

        @JsonProperty("service_type_id")
        private Integer serviceTypeId = 2;

        @JsonProperty("client_order_code")
        private String clientOrderCode;

        private List<OrderItem> items;

        @Getter
        @Setter
        public static class OrderItem {
            private String name;
            private Integer quantity;
            private Integer weight;
            private Integer length = 10;
            private Integer width = 10;
            private Integer height = 10;
            private Integer price;
        }
    }
}
