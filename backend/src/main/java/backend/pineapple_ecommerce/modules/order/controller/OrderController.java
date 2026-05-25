package backend.pineapple_ecommerce.modules.order.controller;

import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.modules.order.service.OrderExportService;
import backend.pineapple_ecommerce.modules.order.service.OrderManagementService;
import backend.pineapple_ecommerce.modules.order.service.OrderService;
import backend.pineapple_ecommerce.modules.order.dto.request.BulkOrderStatusRequest;
import backend.pineapple_ecommerce.modules.order.dto.request.CreateOrderRequest;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.modules.order.dto.response.OrderResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.common.enums.PaymentMethod;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Orders", description = "Quản lý đơn hàng")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final UserService  userService;
    private final OrderExportService orderExportService;
    private final OrderManagementService  orderManagementService;

    // ─────────────────────────────────────────────
    // USER
    // ─────────────────────────────────────────────

    @Operation(summary = "Tạo đơn hàng từ giỏ hàng")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = userService.getCurrentUserId();
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đặt hàng thành công"));
    }

    /**
     * NEW — 2.1: Thêm filter status cho getMyOrders.
     * ?status=DELIVERED để xem đơn đã giao; không truyền = tất cả.
     */
    @Operation(summary = "Lịch sử đơn hàng của tôi (có filter status)")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getMyOrders(userId, status, page, size)));
    }

    @Operation(summary = "Chi tiết đơn hàng của tôi")
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(orderId, userId)));
    }

    @Operation(summary = "Huỷ đơn hàng (chỉ khi PENDING)")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelOrder(orderId, userId), "Đã huỷ đơn hàng"));
    }

    @Operation(summary = "Yêu cầu hoàn tiền (đơn đã thanh toán online)")
    @PostMapping("/{orderId}/request-refund")
    public ResponseEntity<ApiResponse<OrderResponse>> requestRefund(@PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                orderService.requestRefund(orderId, userId),
                "Yêu cầu hoàn tiền đã được ghi nhận"));
    }

    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    /**
     * NEW — 2.1: Filter đa điều kiện (status + userId + paymentMethod + dateRange).
     */
    @Operation(summary = "Lấy tất cả đơn hàng (Admin) — filter đa điều kiện")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.getAllOrders(status, userId, paymentMethod, from, to, page, size)));
    }

    @Operation(summary = "Cập nhật trạng thái đơn hàng (Admin)")
    @PatchMapping("/admin/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateOrderStatus(orderId, status), "Cập nhật trạng thái thành công"));
    }

    /**
     * NEW — 2.1: Bulk update nhiều đơn cùng lúc.
     * Trả về số đơn cập nhật thành công (các đơn transition không hợp lệ bị bỏ qua).
     */
    @Operation(summary = "Cập nhật trạng thái nhiều đơn hàng cùng lúc (Admin)")
    @PostMapping("/admin/bulk-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> bulkUpdateStatus(
            @Valid @RequestBody BulkOrderStatusRequest request) {
        int updated = orderManagementService.bulkUpdateStatus(request);
        return ResponseEntity.ok(ApiResponse.success(updated,
                "Đã cập nhật " + updated + "/" + request.getOrderIds().size() + " đơn hàng thành công"));
    }

    /**
     * Xuất danh sách đơn hàng ra CSV.
     * Hỗ trợ tất cả filter giống GET /admin.
     * GET /api/v1/orders/admin/export?format=csv&status=DELIVERED&from=2025-01-01T00:00:00
     */
    @Operation(summary = "Xuất danh sách đơn hàng ra CSV (Admin)")
    @GetMapping("/admin/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            byte[] data = orderExportService.exportToExcel(status, userId, paymentMethod, from, to);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"orders_" + System.currentTimeMillis() + ".xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }

        // Default: CSV
        byte[] data = orderExportService.exportToCsv(status, userId, paymentMethod, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"orders_" + System.currentTimeMillis() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }
}
