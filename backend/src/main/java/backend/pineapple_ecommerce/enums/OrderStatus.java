package backend.pineapple_ecommerce.enums;

public enum OrderStatus {
    PENDING,        // Chờ xác nhận
    CONFIRMED,      // Đã xác nhận
    PROCESSING,     // Đang chuẩn bị
    SHIPPING,       // Đang giao
    DELIVERED,      // Đã giao
    CANCELLED,      // Đã huỷ
    RETURNED,        // Hoàn hàng

    // Hoàn tiền
    REFUND_REQUESTED,  // Khách yêu cầu hoàn tiền
    REFUNDED           // Đã hoàn tiền
}
