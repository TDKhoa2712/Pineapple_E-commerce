package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.CreateOrderRequest;
import backend.pineapple_ecommerce.dto.response.ApiResponse;
import backend.pineapple_ecommerce.dto.response.OrderResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.service.OrderService;
import backend.pineapple_ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders", description = "Quản lý đơn hàng")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final UserService  userService;

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

    @Operation(summary = "Lịch sử đơn hàng của tôi")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(userId, page, size)));
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

    @Operation(summary = "Yêu cầu hoàn tiền (User)",
            description = "Dành cho đơn hàng đã thanh toán online nhưng bị huỷ",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{orderId}/request-refund")
    public ResponseEntity<ApiResponse<OrderResponse>> requestRefund(@PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                orderService.requestRefund(orderId, userId), "Yêu cầu hoàn tiền đã được ghi nhận"));
    }

    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy tất cả đơn hàng (Admin), lọc theo status")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(status, page, size)));
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
}
