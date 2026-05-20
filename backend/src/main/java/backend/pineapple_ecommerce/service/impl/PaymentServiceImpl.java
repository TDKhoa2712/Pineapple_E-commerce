package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.response.PaymentResponse;
import backend.pineapple_ecommerce.entity.Order;
import backend.pineapple_ecommerce.entity.Payment;
import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.enums.PaymentMethod;
import backend.pineapple_ecommerce.enums.PaymentStatus;
import backend.pineapple_ecommerce.event.EmailEvents;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.mapper.OrderMapper;
import backend.pineapple_ecommerce.repository.OrderRepository;
import backend.pineapple_ecommerce.repository.PaymentRepository;
import backend.pineapple_ecommerce.service.PaymentService;
import backend.pineapple_ecommerce.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

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

    // ─── VNPAY config ──────────────────────────────────────────────────────────
    @Value("${app.vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${app.vnpay.hash-secret}")
    private String secretKey;

    @Value("${app.vnpay.pay-url}")
    private String vnp_PayUrl;

    @Value("${app.vnpay.return-url}")
    private String vnp_ReturnUrl;

    // ─── Frontend base URL (dùng cho Return redirect) ─────────────────────────
    // Thêm vào application-dev.yml:  app.vnpay.frontend-result-url: http://localhost:3000/payment/result
    // Thêm vào application-prod.yml: app.vnpay.frontend-result-url: https://yourdomain.com/payment/result
    @Value("${app.vnpay.frontend-result-url}")
    private String frontendResultUrl;

    // ─── Dependencies ─────────────────────────────────────────────────────────
    private final PaymentRepository      paymentRepository;
    private final OrderRepository        orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderMapper            orderMapper;

    // ══════════════════════════════════════════════════════════════════════════
    // 1. INITIATE PAYMENT
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long orderId, Long userId,
                                           HttpServletRequest request) {
        Order order = findOrderAndVerifyOwner(orderId, userId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng đã được thanh toán");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        String paymentUrl = null;

        if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
            String txnRef = "PNP_" + orderId + "_" + System.currentTimeMillis();
            payment.setTransactionCode(txnRef);
            paymentRepository.save(payment);

            paymentUrl = buildVnPayUrl(payment.getAmount(), txnRef, request);
            log.info("[VNPAY] Payment URL generated — orderId={}, txnRef={}", orderId, txnRef);
        }

        return toResponse(payment, paymentUrl);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. IPN HANDLER — NƠI DUY NHẤT GHI DATABASE
    //
    // Quy trình chuẩn VNPAY:
    //   Xác minh chữ ký → kiểm tra đơn tồn tại → kiểm tra số tiền
    //   → idempotency check → cập nhật trạng thái → publish email event
    //   → trả body chuẩn VNPAY (luôn HTTP 200)
    //
    // Tại sao luôn HTTP 200?
    //   VNPAY chỉ đọc RspCode trong body để biết thành công hay lỗi.
    //   Nếu trả non-200 → VNPAY tưởng server chết → retry vô hạn.
    // ══════════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════════
    // 2. IPN HANDLER
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public Map<String, String> handleVnpayIpn(HttpServletRequest request) {

        // ── Bước 1: Trích xuất tham số nguyên bản để xác minh chữ ký ───────────
        Map<String, String> signParams = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            // Chỉ lấy các param có giá trị thực (chuẩn VNPAY: length > 0)
            if (fieldValue != null && fieldValue.length() > 0) {
                signParams.put(fieldName, fieldValue);
            }
        }

        String receivedHash = signParams.get("vnp_SecureHash");
        signParams.remove("vnp_SecureHashType");
        signParams.remove("vnp_SecureHash");

        String computedHash = VNPayUtil.hashAllFields(signParams, secretKey);

        if (computedHash == null || !computedHash.equals(receivedHash)) {
            log.error("[IPN] Chữ ký không hợp lệ — có thể bị giả mạo");
            return ipnResponse("97", "Invalid Signature");
        }

        // ── Bước 2: Kiểm tra đơn hàng tồn tại ───────────────────────────────
        String txnRef = request.getParameter("vnp_TxnRef");
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionCode(txnRef);

        if (paymentOpt.isEmpty()) {
            log.warn("[IPN] Không tìm thấy payment — txnRef={}", txnRef);
            return ipnResponse("01", "Order Not Found");
        }

        Payment payment = paymentOpt.get();
        Order   order   = payment.getOrder();

        // ── Bước 3: Kiểm tra số tiền khớp ────────────────────────────────────
        String amountParam = request.getParameter("vnp_Amount");
        long vnpAmount   = Long.parseLong(amountParam != null ? amountParam : "0");
        long orderAmount = order.getTotalAmount()
                .multiply(new BigDecimal("100"))
                .longValue();

        if (vnpAmount != orderAmount) {
            log.error("[IPN] Số tiền không khớp — txnRef={}, expected={}, received={}",
                    txnRef, orderAmount, vnpAmount);
            return ipnResponse("04", "Invalid Amount");
        }

        // ── Bước 4: Idempotency — đã xử lý rồi thì trả thành công, không ghi đè ──
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("[IPN] Đơn đã thanh toán trước đó — txnRef={} (bỏ qua retry)", txnRef);
            return ipnResponse("00", "Confirm Success");
        }

        // ── Bước 5: Cập nhật trạng thái ─────────────────────────────────────
        String responseCode = request.getParameter("vnp_ResponseCode");
        payment.setRawResponse(signParams.toString());

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);

            orderRepository.save(order);
            paymentRepository.save(payment);

            log.info("[IPN] Thanh toán thành công — txnRef={}, orderId={}", txnRef, order.getId());

            // ── Bước 6: Gửi email xác nhận (async, AFTER_COMMIT) ─────────────
            publishPaymentSuccessEmail(order);

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("[IPN] Thanh toán thất bại — txnRef={}, vnpCode={}", txnRef, responseCode);
        }

        return ipnResponse("00", "Confirm Success");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. RETURN URL REDIRECT — KHÔNG GHI DATABASE
    //
    // Chỉ đọc responseCode từ query param rồi redirect về Frontend.
    // FE tự gọi GET /api/v1/payments/order/{orderId} để lấy trạng thái thật.
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public String buildReturnRedirectUrl(String vnpResponseCode, String txnRef) {
        String status = "00".equals(vnpResponseCode) ? "success" : "failed";

        return UriComponentsBuilder.fromUriString(frontendResultUrl)
                .queryParam("status", status)
                .queryParam("txnRef", txnRef)
                .queryParam("code", vnpResponseCode)
                .build()
                .toUriString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. COD PAYMENT
    // ══════════════════════════════════════════════════════════════════════════

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

        log.info("[COD] Payment confirmed — orderId={}", orderId);
        return toResponse(payment, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. QUERY
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return toResponse(payment, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Publish event thanh toán thành công để gửi email xác nhận.
     *
     * <p>Event được xử lý bởi {@link backend.pineapple_ecommerce.event.EmailEventListener}
     * với {@code phase = AFTER_COMMIT} → email chỉ gửi sau khi transaction đã commit.
     * Method trong EmailService được đánh {@code @Async} → chạy trên thread riêng,
     * không block IPN response.
     */
    private void publishPaymentSuccessEmail(Order order) {
        try {
            String toEmail = order.getUser().getEmail();
            eventPublisher.publishEvent(
                    new EmailEvents.OrderStatusChangedEvent(
                            toEmail,
                            orderMapper.toResponse(order),
                            "Đã thanh toán - Đang xác nhận"
                    )
            );
        } catch (Exception ex) {
            // Lỗi publish email không được ảnh hưởng đến IPN response
            log.error("[IPN] Không thể publish email event — orderId={}: {}",
                    order.getId(), ex.getMessage());
        }
    }

    private Order findOrderAndVerifyOwner(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền thao tác đơn hàng này");
        }
        return order;
    }

    /**
     * Tạo body response chuẩn VNPAY.
     * VNPAY đọc RspCode để quyết định có retry không.
     * Luôn trả HTTP 200, chỉ thay đổi RspCode.
     */
    private Map<String, String> ipnResponse(String rspCode, String message) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("RspCode", rspCode);
        result.put("Message", message);
        return result;
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

    private String buildVnPayUrl(BigDecimal amount,
                                 String txnRef,
                                 HttpServletRequest request) {

        long amountInVND =
                amount.multiply(new BigDecimal("100"))
                        .longValue();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVND));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", txnRef);

        vnp_Params.put(
                "vnp_OrderInfo",
                "Thanh toan don hang " + txnRef
        );

        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");

        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);

        vnp_Params.put(
                "vnp_IpAddr",
                VNPayUtil.getIpAddress(request)
        );

        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyyMMddHHmmss");

        formatter.setTimeZone(
                TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        );

        Date now = new Date();

        vnp_Params.put(
                "vnp_CreateDate",
                formatter.format(now)
        );

        Calendar cld =
                Calendar.getInstance(
                        TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
                );

        cld.setTime(now);

        cld.add(Calendar.MINUTE, 15);

        vnp_Params.put(
                "vnp_ExpireDate",
                formatter.format(cld.getTime())
        );

        // =========================================
        // SORT FIELD
        // =========================================

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        List<String> hashFieldList  = new ArrayList<>();
        List<String> queryFieldList = new ArrayList<>();

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                //  Hash: key không encode, value dùng US_ASCII (giữ "+")
                hashFieldList.add(fieldName + "="
                        + URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                // Query URL: encode cả key+value bằng UTF-8
                queryFieldList.add(
                        URLEncoder.encode(fieldName, StandardCharsets.UTF_8).replace("+", "%20")
                                + "="
                                + URLEncoder.encode(fieldValue, StandardCharsets.UTF_8).replace("+", "%20")
                );
            }
        }

        String hashData = String.join("&", hashFieldList);
        String query    = String.join("&", queryFieldList);

        // =========================================
        // SECURE HASH
        // =========================================

        String secureHash =
                VNPayUtil.hmacSHA512(
                        secretKey,
                        hashData
                );

//      query += "&vnp_SecureHashType=HMACSHA512";
        query += "&vnp_SecureHash=" + secureHash;
        // =========================================
        // DEBUG
        // =========================================

//        log.info("HASH DATA (raw): [{}]", hashData);
//        log.info("HASH DATA length: {}", hashData.length());
//        log.info("SECURE HASH length: {}", secureHash.length());
//
//        log.info("========== VNPAY REQUEST ==========");
//        log.info("HASH DATA: {}", hashData);
//        log.info("SECURE HASH: {}", secureHash);
//        log.info("FINAL URL: {}", vnp_PayUrl + "?" + query);
//        log.info("===================================");

        return vnp_PayUrl + "?" + query;
    }
}