package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.response.PaymentResponse;
import backend.pineapple_ecommerce.entity.Order;
import backend.pineapple_ecommerce.entity.Payment;
import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.enums.PaymentMethod;
import backend.pineapple_ecommerce.enums.PaymentStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.repository.OrderRepository;
import backend.pineapple_ecommerce.repository.PaymentRepository;
import backend.pineapple_ecommerce.service.PaymentService;
import backend.pineapple_ecommerce.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${app.vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${app.vnpay.hash-secret}")
    private String secretKey;

    @Value("${app.vnpay.pay-url}")
    private String vnp_PayUrl;

    @Value("${app.vnpay.return-url}")
    private String vnp_ReturnUrl;

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long orderId, Long userId, HttpServletRequest request) {
        Order order = findOrderAndVerifyOwner(orderId, userId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng đã được thanh toán");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        String paymentUrl = null;

        if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
            // Cập nhật transaction code (mã giao dịch tự sinh)
            String txnRef = "PNP_" + orderId + "_" + System.currentTimeMillis();
            payment.setTransactionCode(txnRef);
            paymentRepository.save(payment);

            paymentUrl = buildVnPayUrl(payment.getAmount(), txnRef, request);
            log.info("Payment URL generated for VNPAY, orderId={}", orderId);
        }
        // ... Xử lý MoMo

        return toResponse(payment, paymentUrl);
    }

    @Override
    @Transactional
    public void handlePaymentCallback(String provider, Map<String, String> params) {
        if ("VNPAY".equals(provider)) {
            String vnp_SecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHashType");
            params.remove("vnp_SecureHash");

            // Hash lại các param trả về để kiểm tra tính toàn vẹn
            String signValue = VNPayUtil.hashAllFields(params, secretKey);

            if (signValue.equals(vnp_SecureHash)) {
                String txnRef = params.get("vnp_TxnRef");
                String responseCode = params.get("vnp_ResponseCode");

                Payment payment = paymentRepository.findByTransactionCode(txnRef)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionCode", txnRef));

                if ("00".equals(responseCode)) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());

                    Order order = payment.getOrder();
                    order.setPaymentStatus(PaymentStatus.PAID);
                    order.setStatus(OrderStatus.CONFIRMED);

                    orderRepository.save(order);
                    log.info("VNPAY thanh toán thành công: txnRef={}", txnRef);
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                    log.warn("VNPAY thanh toán thất bại: txnRef={}, code={}", txnRef, responseCode);
                }
                payment.setRawResponse(params.toString());
                paymentRepository.save(payment);
            } else {
                log.error("Sai chữ ký bảo mật từ VNPAY!");
                throw new BusinessException("Chữ ký bảo mật không hợp lệ");
            }
        }
    }

    @Override
    @Transactional
    public PaymentResponse confirmCodPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new BusinessException("Đơn hàng không sử dụng phương thức COD");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Chỉ xác nhận thanh toán COD khi đơn đã giao thành công");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        log.info("COD payment confirmed for orderId={}", orderId);
        return toResponse(payment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return toResponse(payment, null);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Order findOrderAndVerifyOwner(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền thao tác đơn hàng này");
        }
        return order;
    }

    private PaymentResponse toResponse(Payment payment, String paymentUrl) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .provider(payment.getProvider().name())
                .transactionCode(payment.getTransactionCode())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .paymentUrl(paymentUrl)
                .build();
    }

    private String buildVnPayUrl(BigDecimal amount, String txnRef, HttpServletRequest request) {
        long amountInVND = amount.multiply(new BigDecimal("100")).longValue(); // VNPAY yêu cầu nhân 100

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVND));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + txnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+7"));
        Date now = new Date();
        vnp_Params.put("vnp_CreateDate", formatter.format(now));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Sắp xếp param theo thứ tự bảng chữ cái (bắt buộc)
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (!fieldName.equals(fieldNames.getLast())) {
                    query.append("&");
                    hashData.append("&");
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayUtil.hmacSHA512(secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnp_PayUrl + "?" + queryUrl;
    }
}
