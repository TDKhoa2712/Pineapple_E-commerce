package backend.pineapple_ecommerce.modules.inventory.service;

import backend.pineapple_ecommerce.modules.inventory.dto.response.InventoryReportResponse;

import java.time.LocalDate;

/**
 * Báo cáo nhập/xuất kho theo khoảng thời gian.
 * Dành cho FARMER và ADMIN xem tình hình hàng hóa.
 */
public interface InventoryReportService {

    /**
     * Tạo báo cáo tổng hợp nhập/xuất kho.
     *
     * @param from ngày bắt đầu (null = từ đầu)
     * @param to   ngày kết thúc (null = đến hiện tại)
     * @return báo cáo gồm tổng hợp và chi tiết theo sản phẩm
     */
    InventoryReportResponse generateReport(LocalDate from, LocalDate to, String groupBy);
}