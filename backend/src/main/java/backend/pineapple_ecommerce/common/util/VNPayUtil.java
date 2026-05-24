package backend.pineapple_ecommerce.common.util;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class VNPayUtil {

    private VNPayUtil() {
    }

    /**
     * Generate HMAC SHA512
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }

            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"
            );
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            char[] hexChars = new char[result.length * 2];
            for (int i = 0; i < result.length; i++) {
                int v = result[i] & 0xFF;
                hexChars[i * 2]     = "0123456789abcdef".charAt(v >>> 4);
                hexChars[i * 2 + 1] = "0123456789abcdef".charAt(v & 0x0F);
            }
            return new String(hexChars);

        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * Get client IP address
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = request.getHeader("x-forwarded-for");
        }

        if (ipAddress == null
                || ipAddress.isBlank()
                || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        if (ipAddress == null) {
            ipAddress = "";
        }

        // localhost IPv6 -> IPv4
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }

        // nhiều proxy IP — lấy IP đầu tiên
        if (ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    /**
     * Build hash data cho VNPAY IPN verification.
     *
     * Dùng UTF_8 và replace "+" → "%20"
     *    để đồng nhất với cách build URL ở buildVnPayUrl().
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        Map<String, String> sortedFields = new TreeMap<>(fields);

        String hashData = sortedFields.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> {
                    //  KHÔNG encode, value encode US_ASCII (giữ nguyên "+")
                    // QUAN TRỌNG: Dùng US_ASCII (không phải UTF-8) để giữ nguyên ký tự "+"
                    // VNPay sandbox encode space thành "+" — nếu đổi sang UTF-8 sẽ ra "%20"
                    // làm sai chữ ký HMAC so với phía VNPay tính
                    String encodedValue = URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII);
                    return e.getKey() + "=" + encodedValue;
                })
                .collect(Collectors.joining("&"));

        return hmacSHA512(secretKey, hashData);
    }

    // ═══════════════════════════════════════════════════════════════
    // VERIFY HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verify chữ ký từ một Map params (dùng cho IPN — đã tách params sẵn).
     *
     * @param params       toàn bộ params nhận được (bao gồm vnp_SecureHash)
     * @param secretKey    hash secret từ config
     * @return true nếu chữ ký hợp lệ
     */
    public static boolean verifyIpnSignature(Map<String, String> params, String secretKey) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        // Loại bỏ 2 field không tham gia tính hash
        Map<String, String> signParams = new TreeMap<>(params);
        signParams.remove("vnp_SecureHash");
        signParams.remove("vnp_SecureHashType");

        String computedHash = hashAllFields(signParams, secretKey);
        // So sánh case-insensitive — VNPay đôi khi trả về uppercase
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    /**
     * Verify chữ ký từ HttpServletRequest (dùng cho Return URL).
     * Tự extract params từ request, loại bỏ SecureHash rồi tính lại.
     *
     * @param request   HttpServletRequest từ VNPay redirect
     * @param secretKey hash secret từ config
     * @return true nếu chữ ký hợp lệ
     */
    public static boolean verifyReturnSignature(HttpServletRequest request, String secretKey) {
        Map<String, String> params = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String value = (entry.getValue() != null && entry.getValue().length > 0)
                    ? entry.getValue()[0] : "";
            if (!value.isEmpty()) {
                params.put(entry.getKey(), value);
            }
        }
        return verifyIpnSignature(params, secretKey);  // logic giống nhau
    }

    // ═══════════════════════════════════════════════════════════════
    // BUILD QUERY STRING (dùng khi tạo payment URL)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Build chuỗi query cho payment URL: sort + encode UTF-8 + join.
     * Phải đồng nhất encoding với {@link #hashAllFields} để hash khớp.
     */
    public static String buildQueryString(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        .replace("+", "%20")
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                        .replace("+", "%20"))
                .collect(Collectors.joining("&"));
    }
}