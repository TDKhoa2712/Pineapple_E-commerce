package backend.pineapple_ecommerce.modules.order.service;

import backend.pineapple_ecommerce.modules.order.repository.OrderRepository;
import backend.pineapple_ecommerce.modules.order.specification.OrderSpecification;
import backend.pineapple_ecommerce.common.enums.OrderStatus;
import backend.pineapple_ecommerce.modules.order.models.Order;
import backend.pineapple_ecommerce.common.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExportServiceImpl implements OrderExportService {

    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─────────────────────────────────────────────
    // CSV EXPORT
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToCsv(
            OrderStatus status,
            Long userId,
            PaymentMethod paymentMethod,
            LocalDateTime from,
            LocalDateTime to) {

        List<Order> orders = fetchOrders(status, userId, paymentMethod, from, to);
        log.info("Exporting {} orders to CSV", orders.size());

        StringBuilder sb = new StringBuilder();

        // UTF-8 BOM — giúp Excel Windows tự nhận dạng encoding
        sb.append('\uFEFF');

        // Header row
        sb.append("Mã đơn hàng,Ngày đặt,Khách hàng,Email,")
                .append("Trạng thái,PTTT,Trạng thái thanh toán,")
                .append("Địa chỉ giao hàng,Sản phẩm,")
                .append("Tạm tính,Phí vận chuyển,Giảm giá,Tổng tiền,Ghi chú\n");

        for (Order order : orders) {
            String productSummary = order.getItems().stream()
                    .map(i -> i.getProductName() + " x" + i.getQuantity())
                    .reduce((a, b) -> a + " | " + b)
                    .orElse("");

            sb.append(order.getId()).append(',')
                    .append(formatDate(order.getCreatedAt())).append(',')
                    .append(csvEscape(order.getUser().getFullName())).append(',')
                    .append(csvEscape(order.getUser().getEmail())).append(',')
                    .append(order.getStatus()).append(',')
                    .append(order.getPaymentMethod()).append(',')
                    .append(order.getPaymentStatus()).append(',')
                    .append(csvEscape(order.getShippingAddress())).append(',')
                    .append(csvEscape(productSummary)).append(',')
                    .append(order.getSubtotal()).append(',')
                    .append(order.getShippingFee()).append(',')
                    .append(order.getDiscountAmount()).append(',')
                    .append(order.getTotalAmount()).append(',')
                    .append(csvEscape(order.getNote()))
                    .append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ─────────────────────────────────────────────
    // EXCEL EXPORT
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(
            OrderStatus status,
            Long userId,
            PaymentMethod paymentMethod,
            LocalDateTime from,
            LocalDateTime to) {

        List<Order> orders = fetchOrders(status, userId, paymentMethod, from, to);
        log.info("Exporting {} orders to Excel", orders.size());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders");

            // ── Styles ──
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle moneyStyle  = buildMoneyStyle(workbook);
            CellStyle dateStyle   = buildDateStyle(workbook);

            // ── Header row ──
            String[] headers = {
                    "Mã ĐH", "Ngày đặt", "Khách hàng", "Email",
                    "Trạng thái", "PTTT", "TT Thanh toán",
                    "Địa chỉ giao hàng", "Sản phẩm",
                    "Tạm tính", "Phí ship", "Giảm giá", "Tổng tiền", "Ghi chú"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Freeze header row
            sheet.createFreezePane(0, 1);

            // ── Data rows ──
            int rowNum = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);

                String productSummary = order.getItems().stream()
                        .map(i -> i.getProductName() + " x" + i.getQuantity())
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");

                row.createCell(0).setCellValue(order.getId());
                Cell dateCell = row.createCell(1);
                dateCell.setCellValue(formatDate(order.getCreatedAt()));
                dateCell.setCellStyle(dateStyle);

                row.createCell(2).setCellValue(order.getUser().getFullName());
                row.createCell(3).setCellValue(order.getUser().getEmail());
                row.createCell(4).setCellValue(order.getStatus().name());
                row.createCell(5).setCellValue(order.getPaymentMethod().name());
                row.createCell(6).setCellValue(order.getPaymentStatus().name());
                row.createCell(7).setCellValue(safeStr(order.getShippingAddress()));
                Cell productCell = row.createCell(8);
                productCell.setCellValue(productSummary);

                Cell subtotalCell = row.createCell(9);
                subtotalCell.setCellValue(order.getSubtotal().doubleValue());
                subtotalCell.setCellStyle(moneyStyle);

                Cell shippingCell = row.createCell(10);
                shippingCell.setCellValue(order.getShippingFee().doubleValue());
                shippingCell.setCellStyle(moneyStyle);

                Cell discountCell = row.createCell(11);
                discountCell.setCellValue(order.getDiscountAmount().doubleValue());
                discountCell.setCellStyle(moneyStyle);

                Cell totalCell = row.createCell(12);
                totalCell.setCellValue(order.getTotalAmount().doubleValue());
                totalCell.setCellStyle(moneyStyle);

                row.createCell(13).setCellValue(safeStr(order.getNote()));
            }

            // Auto-size columns (bỏ qua col sản phẩm và địa chỉ vì quá dài)
            for (int i = 0; i < headers.length; i++) {
                if (i != 7 && i != 8) {
                    sheet.autoSizeColumn(i);
                } else {
                    sheet.setColumnWidth(i, 10000); // ~35 chars
                }
            }

            // Summary row
            Row summaryRow = sheet.createRow(rowNum + 1);
            summaryRow.createCell(11).setCellValue("Tổng doanh thu:");
            BigDecimal grandTotal = orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Cell grandCell = summaryRow.createCell(12);
            grandCell.setCellValue(grandTotal.doubleValue());
            grandCell.setCellStyle(moneyStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private List<Order> fetchOrders(
            OrderStatus status,
            Long userId,
            PaymentMethod paymentMethod,
            LocalDateTime from,
            LocalDateTime to) {

        Specification<Order> spec =
                OrderSpecification.hasStatus(status)
                        .and(OrderSpecification.hasUserId(userId))
                        .and(OrderSpecification.hasPaymentMethod(paymentMethod))
                        .and(OrderSpecification.createdBetween(from, to));
        // Lấy tất cả (không phân trang) — dùng cho export
        return orderRepository.findAll(spec);
    }

    /** Escape giá trị cho CSV: wrap bằng nháy kép nếu chứa dấu phẩy, xuống dòng, hoặc nháy kép. */
    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatDate(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "";
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(headerFont);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle buildMoneyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle buildDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}