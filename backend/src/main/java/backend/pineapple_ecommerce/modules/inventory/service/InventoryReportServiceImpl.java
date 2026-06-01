package backend.pineapple_ecommerce.modules.inventory.service;

import backend.pineapple_ecommerce.common.enums.BatchStatus;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import backend.pineapple_ecommerce.modules.inventory.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventoryReportResponse;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventoryReportResponse.ProductReportDetail;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventoryReportResponse.ReportSummary;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReportServiceImpl implements InventoryReportService {

    private final InventoryBatchRepository inventoryBatchRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryReportResponse generateReport(LocalDate from, LocalDate to, String groupBy) {
        // Validate
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(LocalTime.MAX) : null;

        log.info("Generating optimized inventory report from={} to={}, groupBy={}", from, to, groupBy);

        // Fetch aggregated values from optimized DB queries
        List<Object[]> importReport = inventoryBatchRepository.findImportReportRaw(fromDt, toDt);
        List<Object[]> salesReport  = inventoryBatchRepository.findSalesReportRaw(fromDt, toDt);
        List<Object[]> expiryReport = inventoryBatchRepository.findExpiryReportRaw(from, to);
        List<Object[]> importDates  = inventoryBatchRepository.findImportDatesRaw(fromDt, toDt);

        // Map containing consolidated builders for each active product ID
        Map<Long, ProductReportDetail.ProductReportDetailBuilder> detailBuilders = new java.util.LinkedHashMap<>();
        Map<Long, String> productNames = new java.util.HashMap<>();

        // Process imports
        for (Object[] row : importReport) {
            Long productId = (Long) row[0];
            String name = (String) row[1];
            long batches = ((Number) row[2]).longValue();
            long qty = ((Number) row[3]).longValue();

            productNames.put(productId, name);
            detailBuilders.computeIfAbsent(productId, k -> ProductReportDetail.builder().productId(k))
                    .batchesImported(batches)
                    .quantityImported(qty);
        }

        // Process sales
        for (Object[] row : salesReport) {
            Long productId = (Long) row[0];
            String name = (String) row[1];
            long qty = ((Number) row[2]).longValue();

            productNames.putIfAbsent(productId, name);
            detailBuilders.computeIfAbsent(productId, k -> ProductReportDetail.builder().productId(k))
                    .quantitySold(qty);
        }

        // Process expirations
        for (Object[] row : expiryReport) {
            Long productId = (Long) row[0];
            String name = (String) row[1];
            long batches = ((Number) row[2]).longValue();
            long qty = ((Number) row[3]).longValue();

            productNames.putIfAbsent(productId, name);
            detailBuilders.computeIfAbsent(productId, k -> ProductReportDetail.builder().productId(k))
                    .batchesExpired(batches)
                    .quantityExpired(qty);
        }

        // Process import date ranges
        for (Object[] row : importDates) {
            Long productId = (Long) row[0];
            LocalDateTime earliest = (LocalDateTime) row[1];
            LocalDateTime latest = (LocalDateTime) row[2];

            detailBuilders.computeIfAbsent(productId, k -> ProductReportDetail.builder().productId(k))
                    .earliestImport(earliest != null ? earliest.toLocalDate() : null)
                    .latestImport(latest != null ? latest.toLocalDate() : null);
        }

        // Fetch current stock for active products to avoid N+1
        List<Long> productIds = new ArrayList<>(detailBuilders.keySet());
        Map<Long, Integer> stockMap = new java.util.HashMap<>();
        if (!productIds.isEmpty()) {
            List<Object[]> stockResults = inventoryBatchRepository.getTotalAvailableStockByProductIds(productIds);
            stockMap = stockResults.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            row -> (Long) row[0],
                            row -> ((Number) row[1]).intValue()
                    ));
        }

        // Build list of details and calculate summary metrics
        List<ProductReportDetail> details = new ArrayList<>();
        long totalImported     = 0;
        long totalQtyImported  = 0;
        long totalQtySold      = 0;
        long totalExpiredBatch = 0;
        long totalQtyExpired   = 0;

        for (Map.Entry<Long, ProductReportDetail.ProductReportDetailBuilder> entry : detailBuilders.entrySet()) {
            Long productId = entry.getKey();
            ProductReportDetail.ProductReportDetailBuilder builder = entry.getValue();

            String name = productNames.getOrDefault(productId, "Sản phẩm #" + productId);
            Integer stock = stockMap.getOrDefault(productId, 0);

            ProductReportDetail detail = builder
                    .productName(name)
                    .currentStock(stock)
                    .build();

            details.add(detail);

            totalImported     += detail.getBatchesImported();
            totalQtyImported  += detail.getQuantityImported();
            totalQtySold      += detail.getQuantitySold();
            totalExpiredBatch += detail.getBatchesExpired();
            totalQtyExpired   += detail.getQuantityExpired();
        }

        // Fetch daily timeline data from DB
        List<Object[]> importTimeline = inventoryBatchRepository.findImportTimelineRaw(fromDt, toDt);
        List<Object[]> salesTimeline  = inventoryBatchRepository.findSalesTimelineRaw(fromDt, toDt);

        Map<LocalDate, Long> importMap = new java.util.HashMap<>();
        for (Object[] row : importTimeline) {
            LocalDate date = (LocalDate) row[0];
            long qty = ((Number) row[1]).longValue();
            importMap.put(date, qty);
        }

        Map<LocalDate, Long> salesMap = new java.util.HashMap<>();
        for (Object[] row : salesTimeline) {
            LocalDate date = (LocalDate) row[0];
            long qty = ((Number) row[1]).longValue();
            salesMap.put(date, qty);
        }

        // Determine timeline window
        LocalDate start = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate end = to != null ? to : LocalDate.now();

        if (from == null) {
            LocalDate earliest = LocalDate.now().minusMonths(1);
            for (LocalDate d : importMap.keySet()) {
                if (d.isBefore(earliest)) earliest = d;
            }
            for (LocalDate d : salesMap.keySet()) {
                if (d.isBefore(earliest)) earliest = d;
            }
            start = earliest;
        }

        // Generate baseline daily timeline (filling zeroes where there is no activity)
        List<InventoryReportResponse.ReportTimelinePoint> dailyPoints = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            long imported = importMap.getOrDefault(date, 0L);
            long sold = salesMap.getOrDefault(date, 0L);
            dailyPoints.add(InventoryReportResponse.ReportTimelinePoint.builder()
                    .date(date)
                    .label(date.toString())
                    .quantityImported(imported)
                    .quantitySold(sold)
                    .build());
        }

        // Process final timeline according to groupBy option
        List<InventoryReportResponse.ReportTimelinePoint> finalTimeline = dailyPoints;
        if ("week".equalsIgnoreCase(groupBy)) {
            Map<LocalDate, long[]> weeklyAccumulator = new java.util.TreeMap<>();
            for (InventoryReportResponse.ReportTimelinePoint dp : dailyPoints) {
                LocalDate monday = dp.getDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                long[] acc = weeklyAccumulator.computeIfAbsent(monday, k -> new long[2]);
                acc[0] += dp.getQuantityImported();
                acc[1] += dp.getQuantitySold();
            }
            finalTimeline = new ArrayList<>();
            for (Map.Entry<LocalDate, long[]> entry : weeklyAccumulator.entrySet()) {
                finalTimeline.add(InventoryReportResponse.ReportTimelinePoint.builder()
                        .date(entry.getKey())
                        .label("Tuần " + entry.getKey().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .quantityImported(entry.getValue()[0])
                        .quantitySold(entry.getValue()[1])
                        .build());
            }
        } else if ("month".equalsIgnoreCase(groupBy)) {
            Map<LocalDate, long[]> monthlyAccumulator = new java.util.TreeMap<>();
            for (InventoryReportResponse.ReportTimelinePoint dp : dailyPoints) {
                LocalDate firstOfMonth = dp.getDate().withDayOfMonth(1);
                long[] acc = monthlyAccumulator.computeIfAbsent(firstOfMonth, k -> new long[2]);
                acc[0] += dp.getQuantityImported();
                acc[1] += dp.getQuantitySold();
            }
            finalTimeline = new ArrayList<>();
            for (Map.Entry<LocalDate, long[]> entry : monthlyAccumulator.entrySet()) {
                finalTimeline.add(InventoryReportResponse.ReportTimelinePoint.builder()
                        .date(entry.getKey())
                        .label(entry.getKey().format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")))
                        .quantityImported(entry.getValue()[0])
                        .quantitySold(entry.getValue()[1])
                        .build());
            }
        }

        // Total available stock across all products (independent of date range)
        long currentAvailableStock = inventoryBatchRepository.sumAllAvailableStock();

        ReportSummary summary = ReportSummary.builder()
                .totalBatchesImported(totalImported)
                .totalQuantityImported(totalQtyImported)
                .totalQuantitySold(totalQtySold)
                .totalBatchesExpired(totalExpiredBatch)
                .totalQuantityExpired(totalQtyExpired)
                .currentAvailableStock(currentAvailableStock)
                .build();

        return InventoryReportResponse.builder()
                .from(from)
                .to(to)
                .summary(summary)
                .details(details)
                .timeline(finalTimeline)
                .build();
    }
}