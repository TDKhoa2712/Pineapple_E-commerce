package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.enums.OrderStatus;
import backend.pineapple_ecommerce.enums.PaymentMethod;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

/**
 * Xuất danh sách đơn hàng ra file CSV hoặc Excel.
 * Admin dùng để tải về báo cáo, nhập vào hệ thống kế toán.
 */
public interface OrderExportService {

    /**
     * Xuất đơn hàng ra CSV (UTF-8 with BOM để Excel mở đúng tiếng Việt).
     *
     * @param status        lọc theo trạng thái (null = tất cả)
     * @param userId        lọc theo user (null = tất cả)
     * @param paymentMethod lọc theo PTTT (null = tất cả)
     * @param from          từ ngày tạo (null = không giới hạn)
     * @param to            đến ngày tạo (null = không giới hạn)
     * @return mảng byte nội dung CSV
     */
    byte[] exportToCsv(
            OrderStatus status,
            Long userId,
            PaymentMethod paymentMethod,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Xuất đơn hàng ra Excel (.xlsx) với Apache POI.
     * Sheet duy nhất "Orders" có header frozen row, auto-size column.
     */
    byte[] exportToExcel(
            OrderStatus status,
            Long userId,
            PaymentMethod paymentMethod,
            LocalDateTime from,
            LocalDateTime to);
}