package backend.pineapple_ecommerce.util;

import backend.pineapple_ecommerce.common.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("VNPayUtil")
class VNPayUtilTest {

    private static final String SECRET_KEY = "testSecretKey123";

    // ─────────────────────────────────────────────────────────────────
    // hmacSHA512
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hmacSHA512()")
    class HmacSha512 {

        @Test
        @DisplayName("với key và data hợp lệ → trả về chuỗi hex 128 ký tự")
        void givenValidKeyAndData_shouldReturn128CharHexString() {
            String result = VNPayUtil.hmacSHA512(SECRET_KEY, "hello");
            assertThat(result)
                    .hasSize(128)
                    .matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("cùng input → luôn cho cùng output (deterministic)")
        void givenSameInput_shouldBeDeterministic() {
            String r1 = VNPayUtil.hmacSHA512(SECRET_KEY, "test-data");
            String r2 = VNPayUtil.hmacSHA512(SECRET_KEY, "test-data");
            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("data khác nhau → hash khác nhau")
        void givenDifferentData_shouldProduceDifferentHash() {
            String r1 = VNPayUtil.hmacSHA512(SECRET_KEY, "data-one");
            String r2 = VNPayUtil.hmacSHA512(SECRET_KEY, "data-two");
            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("key khác nhau → hash khác nhau")
        void givenDifferentKeys_shouldProduceDifferentHash() {
            String r1 = VNPayUtil.hmacSHA512("key-one", "same-data");
            String r2 = VNPayUtil.hmacSHA512("key-two", "same-data");
            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("key null → trả về chuỗi rỗng (không throw)")
        void givenNullKey_shouldReturnEmpty() {
            assertThat(VNPayUtil.hmacSHA512(null, "data")).isEmpty();
        }

        @Test
        @DisplayName("data null → trả về chuỗi rỗng (không throw)")
        void givenNullData_shouldReturnEmpty() {
            assertThat(VNPayUtil.hmacSHA512(SECRET_KEY, null)).isEmpty();
        }

        @Test
        @DisplayName("data rỗng → vẫn trả về hash 128 ký tự hợp lệ")
        void givenEmptyData_shouldReturnValidHash() {
            String result = VNPayUtil.hmacSHA512(SECRET_KEY, "");
            assertThat(result)
                    .hasSize(128)
                    .matches("[0-9a-f]+");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // getIpAddress
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getIpAddress()")
    class GetIpAddress {

        @Test
        @DisplayName("có header X-FORWARDED-FOR → trả về giá trị header")
        void givenForwardedForHeader_shouldReturnHeaderValue() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-FORWARDED-FOR")).thenReturn("203.0.113.42");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            assertThat(VNPayUtil.getIpAddress(request)).isEqualTo("203.0.113.42");
        }

        @Test
        @DisplayName("không có header X-FORWARDED-FOR → fallback sang remoteAddr")
        void givenNoForwardedHeader_shouldFallbackToRemoteAddr() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            assertThat(VNPayUtil.getIpAddress(request)).isEqualTo("127.0.0.1");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // hashAllFields
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hashAllFields()")
    class HashAllFields {

        @Test
        @DisplayName("hash của map fields bằng với hmacSHA512 của chuỗi key=value&key2=value2")
        void givenFields_shouldMatchManualHmac() {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("vnp_Amount", "100000");
            fields.put("vnp_TxnRef", "PNP_001");

            String expected = VNPayUtil.hmacSHA512(SECRET_KEY, "vnp_Amount=100000&vnp_TxnRef=PNP_001");
            String actual = VNPayUtil.hashAllFields(fields, SECRET_KEY);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        @DisplayName("map rỗng → hash của chuỗi rỗng")
        void givenEmptyFields_shouldHashEmptyString() {
            Map<String, String> fields = new LinkedHashMap<>();
            String expected = VNPayUtil.hmacSHA512(SECRET_KEY, "");
            assertThat(VNPayUtil.hashAllFields(fields, SECRET_KEY)).isEqualTo(expected);
        }
    }
}