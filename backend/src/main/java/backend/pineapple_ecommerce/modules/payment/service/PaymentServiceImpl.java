package backend.pineapple_ecommerce.modules.payment.service;

import backend.pineapple_ecommerce.common.enums.PaymentMethod;
import backend.pineapple_ecommerce.modules.payment.repository.PaymentRepository;
import backend.pineapple_ecommerce.common.enums.PaymentStatus;
import backend.pineapple_ecommerce.modules.payment.models.Payment;
import backend.pineapple_ecommerce.modules.payment.dto.response.PaymentResponse;
import backend.pineapple_ecommerce.modules.order.models.Order;
import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.event.EmailEvents;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import backend.pineapple_ecommerce.modules.order.mapper.OrderMapper;
import backend.pineapple_ecommerce.modules.order.repository.OrderRepository;
import backend.pineapple_ecommerce.common.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    // ─── VNPay config ─────────────────────────────────────────────────────────
    @Value("${app.vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${app.vnpay.hash-secret}")
    private String secretKey;

    @Value("${app.vnpay.pay-url}")
    private String vnp_PayUrl;

    @Value("${app.vnpay.return-url}")
    private String vnp_ReturnUrl;

    @Value("${app.vnpay.frontend-result-url}")
    private String frontendResultUrl;

    // ─── Dependencies ─────────────────────────────────────────────────────────
    private final PaymentRepository paymentRepository;
    private final OrderRepository         orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderMapper             orderMapper;

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
            // Tạo txnRef mới cho mỗi lần initiate (đảm bảo unique)
            String txnRef = "PNP_" + orderId + "_" + System.currentTimeMillis();
            payment.setTransactionCode(txnRef);
            paymentRepository.save(payment);

            paymentUrl = buildVnPayUrl(payment.getAmount(), txnRef, request);
            log.info("[VNPAY] Payment URL generated — orderId={}, txnRef={}", orderId, txnRef);
        } else if (order.getPaymentMethod() == PaymentMethod.MOMO) {
            throw new BusinessException("Phương thức thanh toán MoMo hiện chưa được hỗ trợ.");
        } else if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            throw new BusinessException("Phương thức thanh toán chuyển khoản ngân hàng hiện chưa được hỗ trợ.");
        }

        return toResponse(payment, paymentUrl);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. IPN HANDLER — NƠI DUY NHẤT GHI DATABASE
    //
    // Quy trình chuẩn VNPay:
    //   Verify chữ ký → kiểm tra đơn tồn tại → kiểm tra số tiền
    //   → idempotency check → cập nhật trạng thái → publish email event
    //   → trả body chuẩn VNPay (luôn HTTP 200)
    //
    // Luôn trả HTTP 200: VNPay chỉ đọc RspCode trong body.
    //   Non-200 → VNPay tưởng server chết → retry vô hạn.
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public Map<String, String> handleVnpayIpn(HttpServletRequest request) {

        // ── Bước 1: Trích xuất và verify chữ ký ─────────────────────────────
        Map<String, String> params = extractParams(request);

        if (!VNPayUtil.verifyIpnSignature(params, secretKey)) {
            log.error("[IPN] Chữ ký không hợp lệ — có thể bị giả mạo. params={}", params.keySet());
            return ipnResponse("97", "Invalid Signature");
        }

        // ── Bước 2: Kiểm tra đơn hàng tồn tại ───────────────────────────────
        String txnRef = params.get("vnp_TxnRef");
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionCodeForUpdate(txnRef);

        if (paymentOpt.isEmpty()) {
            log.warn("[IPN] Không tìm thấy payment — txnRef={}", txnRef);
            return ipnResponse("01", "Order Not Found");
        }

        Payment payment = paymentOpt.get();
        Order   order   = payment.getOrder();

        // ── Bước 3: Kiểm tra số tiền khớp ────────────────────────────────────
        String amountParam = params.getOrDefault("vnp_Amount", "0");
        long vnpAmount   = Long.parseLong(amountParam);
        long orderAmount = order.getTotalAmount()
                .multiply(new BigDecimal("100"))
                .longValue();

        if (vnpAmount != orderAmount) {
            log.error("[IPN] Số tiền không khớp — txnRef={}, expected={}, received={}",
                    txnRef, orderAmount, vnpAmount);
            return ipnResponse("04", "Invalid Amount");
        }

        // ── Bước 4: Idempotency check ─────────────────────────────────────────
        if (payment.getStatus() != PaymentStatus.UNPAID) {
            log.info("[IPN] Đơn đã xử lý trước đó (status={}) — txnRef={}",
                    payment.getStatus(), txnRef);
            return ipnResponse("00", "Confirm Success");
        }

        // ── Bước 5: Cập nhật trạng thái ──────────────────────────────────────
        String responseCode = params.get("vnp_ResponseCode");
        payment.setRawResponse(params.toString());

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
    // 3. RETURN URL — VERIFY CHỮ KÝ RỒI MỚI REDIRECT
    //
    // Return URL là browser redirect — attacker có thể forge query params để
    // FE hiển thị "thanh toán thành công" giả. Verify chữ ký trước khi
    // build redirect URL để đảm bảo params thực sự đến từ VNPay.
    //
    // DB KHÔNG được chạm ở đây — FE tự gọi GET /payments/order/{id} lấy
    // trạng thái thật từ DB (đã được IPN cập nhật trước đó).
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public String buildReturnRedirectUrl(HttpServletRequest request) {
        // Verify chữ ký trước khi đọc bất kỳ param nào
        boolean signatureValid = VNPayUtil.verifyReturnSignature(request, secretKey);

        if (!signatureValid) {
            log.warn("[Return URL] Chữ ký không hợp lệ — redirect về trang lỗi");
            return UriComponentsBuilder.fromUriString(frontendResultUrl)
                    .queryParam("status", "invalid")
                    .queryParam("error", "signature_mismatch")
                    .build()
                    .toUriString();
        }

        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef       = request.getParameter("vnp_TxnRef");

        log.info("[Return URL] Chữ ký hợp lệ — txnRef={}, responseCode={}", txnRef, responseCode);

        String status = "00".equals(responseCode) ? "success" : "failed";
        return UriComponentsBuilder.fromUriString(frontendResultUrl)
                .queryParam("status", status)
                .queryParam("txnRef", txnRef)
                .queryParam("code", responseCode)
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
    public PaymentResponse getPaymentByOrderId(Long orderId, Long currentUserId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        Order order = payment.getOrder();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!order.getUser().getId().equals(currentUserId) && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền xem thông tin thanh toán của đơn hàng này");
        }

        return toResponse(payment, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Build payment URL gửi sang VNPay.
     *
     * Encoding PHẢI đồng nhất với {@link VNPayUtil#hashAllFields}:
     *   - key: không encode
     *   - value: UTF-8, replace "+" → "%20"
     *   - sort theo alphabet (TreeMap trong VNPayUtil)
     */
    private String buildVnPayUrl(BigDecimal amount, String txnRef, HttpServletRequest request) {
        long amountInVND = amount.multiply(new BigDecimal("100")).longValue();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        Date now = new Date();

        Calendar expiry = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        expiry.setTime(now);
        expiry.add(Calendar.MINUTE, 15);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version",    "2.1.0");
        vnpParams.put("vnp_Command",    "pay");
        vnpParams.put("vnp_TmnCode",    vnp_TmnCode);
        vnpParams.put("vnp_Amount",     String.valueOf(amountInVND));
        vnpParams.put("vnp_CurrCode",   "VND");
        vnpParams.put("vnp_TxnRef",     txnRef);
        vnpParams.put("vnp_OrderInfo",  "Thanh toan don hang " + txnRef); // ASCII only — tránh vấn đề encoding
        vnpParams.put("vnp_OrderType",  "other");
        vnpParams.put("vnp_Locale",     "vn");
        vnpParams.put("vnp_ReturnUrl",  vnp_ReturnUrl);
        vnpParams.put("vnp_IpAddr",     VNPayUtil.getIpAddress(request));
        vnpParams.put("vnp_CreateDate", formatter.format(now));
        vnpParams.put("vnp_ExpireDate", formatter.format(expiry.getTime()));

        // ── Hash data (dùng VNPayUtil để đảm bảo encoding nhất quán) ──────────
        String secureHash = VNPayUtil.hashAllFields(vnpParams, secretKey);

        // ── Query string (UTF-8, encode cả key+value) ──────────────────────────
        String query = VNPayUtil.buildQueryString(vnpParams)
                + "&vnp_SecureHash=" + secureHash;

        return vnp_PayUrl + "?" + query;
    }

    /**
     * Extract tất cả params từ HttpServletRequest vào Map.
     * Bỏ qua param rỗng (chuẩn VNPay).
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String value = (entry.getValue() != null && entry.getValue().length > 0)
                    ? entry.getValue()[0] : "";
            if (!value.isEmpty()) {
                params.put(entry.getKey(), value);
            }
        }
        return params;
    }

    private void publishPaymentSuccessEmail(Order order) {
        try {
            eventPublisher.publishEvent(
                    new EmailEvents.OrderStatusChangedEvent(
                            order.getUser().getEmail(),
                            orderMapper.toResponse(order),
                            "Đã thanh toán - Đang xác nhận"
                    )
            );
        } catch (Exception ex) {
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
}