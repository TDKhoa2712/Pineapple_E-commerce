package backend.pineapple_ecommerce.infrastructure.vnpay;

import backend.pineapple_ecommerce.modules.payment.dto.response.VNPayQueryResult;
import backend.pineapple_ecommerce.modules.payment.models.Payment;
import backend.pineapple_ecommerce.common.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayQueryServiceImpl implements VNPayQueryService {

    @Value("${app.vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${app.vnpay.hash-secret}")
    private String secretKey;

    @Value("${app.vnpay.api-url}")
    private String vnp_ApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gọi API QueryDR của VNPay để lấy trạng thái thật của giao dịch.
     * * @param payment Bản ghi Payment đang bị kẹt cần truy vấn
     * @return VNPayQueryResult chứa trạng thái (00 là thành công)
     */
    @Override
    public VNPayQueryResult queryTransaction(Payment payment) {
        try {
            String vnp_RequestId = UUID.randomUUID().toString();
            String vnp_Version = "2.1.0";
            String vnp_Command = "querydr";
            String vnp_TxnRef = payment.getTransactionCode();
            String vnp_OrderInfo = "Truy van giao dich " + vnp_TxnRef;

            // Format thời gian theo chuẩn yyyyMMddHHmmss
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

            // vnp_TransactionDate LÀ BẮT BUỘC: Phải là thời gian lúc tạo URL thanh toán.
            // Thông thường ta dùng chính thời gian createdAt của bản ghi Payment
            Date createDate = Date.from(payment.getCreatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
            String vnp_TransactionDate = formatter.format(createDate);

            String vnp_CreateDate = formatter.format(new Date()); // Thời gian gọi API Query
            String vnp_IpAddr = "127.0.0.1"; // Có thể lấy IP thật của server nếu có

            // 1. TẠO CHUỖI BĂM (Theo chuẩn QueryDR của VNPay)
            // Cấu trúc: RequestId|Version|Command|TmnCode|TxnRef|TransactionDate|CreateDate|IpAddr|OrderInfo
            String hashData = String.join("|",
                    vnp_RequestId, vnp_Version, vnp_Command, vnp_TmnCode,
                    vnp_TxnRef, vnp_TransactionDate, vnp_CreateDate,
                    vnp_IpAddr, vnp_OrderInfo);

            String vnp_SecureHash = VNPayUtil.hmacSHA512(secretKey, hashData);

            // 2. TẠO BODY JSON REQUEST
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("vnp_RequestId", vnp_RequestId);
            requestBody.put("vnp_Version", vnp_Version);
            requestBody.put("vnp_Command", vnp_Command);
            requestBody.put("vnp_TmnCode", vnp_TmnCode);
            requestBody.put("vnp_TxnRef", vnp_TxnRef);
            requestBody.put("vnp_OrderInfo", vnp_OrderInfo);
            requestBody.put("vnp_TransactionDate", vnp_TransactionDate);
            requestBody.put("vnp_CreateDate", vnp_CreateDate);
            requestBody.put("vnp_IpAddr", vnp_IpAddr);
            requestBody.put("vnp_SecureHash", vnp_SecureHash);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // 3. GỬI REQUEST
            log.info("[VNPay QueryDR] Gửi yêu cầu truy vấn cho txnRef={}", vnp_TxnRef);
            Map<String, String> response = restTemplate.postForObject(vnp_ApiUrl, entity, Map.class);

            if (response == null) {
                throw new RuntimeException("Không nhận được phản hồi từ VNPay");
            }

            // 4. PHÂN TÍCH KẾT QUẢ
            String responseCode = response.get("vnp_ResponseCode"); // 00 là Query thành công
            String txnStatus = response.get("vnp_TransactionStatus"); // 00 là Đã thanh toán, 01 là chưa, 02 là lỗi...
            String message = response.get("vnp_Message");

            log.info("[VNPay QueryDR] txnRef={} -> RspCode: {}, TxnStatus: {}", vnp_TxnRef, responseCode, txnStatus);

            return VNPayQueryResult.builder()
                    .responseCode(responseCode)
                    .transactionStatus(txnStatus)
                    .message(message)
                    .rawResponse(response.toString())
                    .build();

        } catch (Exception e) {
            log.error("[VNPay QueryDR] Lỗi khi truy vấn giao dịch: {}", e.getMessage());
            throw new RuntimeException("Truy vấn VNPay thất bại", e);
        }
    }
}