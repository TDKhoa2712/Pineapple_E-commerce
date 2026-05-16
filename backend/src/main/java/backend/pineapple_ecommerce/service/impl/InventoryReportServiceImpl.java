package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.response.InventoryReportResponse;
import backend.pineapple_ecommerce.dto.response.InventoryReportResponse.ProductReportDetail;
import backend.pineapple_ecommerce.dto.response.InventoryReportResponse.ReportSummary;
import backend.pineapple_ecommerce.entity.InventoryBatch;
import backend.pineapple_ecommerce.enums.BatchStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.service.InventoryReportService;
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
    public InventoryReportResponse generateReport(LocalDate from, LocalDate to) {
        // Validate
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(LocalTime.MAX) : null;

        log.info("Generating inventory report from={} to={}", from, to);

        // Lấy tất cả lô trong kỳ
        List<InventoryBatch> batches =
                inventoryBatchRepository.findByCreatedAtBetween(fromDt, toDt);

        // Tổng tồn kho hiện tại (không phụ thuộc kỳ báo cáo)
        long currentAvailableStock = inventoryBatchRepository.sumAllAvailableStock();

        // ── Nhóm theo sản phẩm ──
        Map<Long, List<InventoryBatch>> byProduct = new LinkedHashMap<>();
        for (InventoryBatch b : batches) {
            byProduct.computeIfAbsent(b.getProduct().getId(), k -> new ArrayList<>()).add(b);
        }

        // ── Build chi tiết theo sản phẩm ──
        List<ProductReportDetail> details = new ArrayList<>();
        long totalImported     = 0;
        long totalQtyImported  = 0;
        long totalQtySold      = 0;
        long totalExpiredBatch = 0;
        long totalQtyExpired   = 0;

        for (Map.Entry<Long, List<InventoryBatch>> entry : byProduct.entrySet()) {
            List<InventoryBatch> pBatches = entry.getValue();
            InventoryBatch first = pBatches.get(0);

            long batchImported = pBatches.size();
            long qtyImported   = pBatches.stream().mapToLong(InventoryBatch::getQuantity).sum();
            long qtySold       = pBatches.stream()
                    .mapToLong(b -> b.getQuantity() - b.getRemainingQuantity())
                    .sum();
            long expiredBatch  = pBatches.stream()
                    .filter(b -> b.getStatus() == BatchStatus.EXPIRED).count();
            long qtyExpired    = pBatches.stream()
                    .filter(b -> b.getStatus() == BatchStatus.EXPIRED)
                    .mapToLong(InventoryBatch::getRemainingQuantity)
                    .sum();

            // Tồn kho riêng của sản phẩm này (chỉ lô AVAILABLE, không giới hạn kỳ)
            Integer stock = inventoryBatchRepository.getTotalAvailableStock(entry.getKey());

            LocalDate earliest = pBatches.stream()
                    .map(b -> b.getCreatedAt().toLocalDate())
                    .min(Comparator.naturalOrder()).orElse(null);
            LocalDate latest = pBatches.stream()
                    .map(b -> b.getCreatedAt().toLocalDate())
                    .max(Comparator.naturalOrder()).orElse(null);

            details.add(ProductReportDetail.builder()
                    .productId(entry.getKey())
                    .productName(first.getProduct().getName())
                    .batchesImported(batchImported)
                    .quantityImported(qtyImported)
                    .quantitySold(qtySold)
                    .batchesExpired(expiredBatch)
                    .quantityExpired(qtyExpired)
                    .currentStock(stock != null ? stock : 0)
                    .earliestImport(earliest)
                    .latestImport(latest)
                    .build());

            totalImported     += batchImported;
            totalQtyImported  += qtyImported;
            totalQtySold      += qtySold;
            totalExpiredBatch += expiredBatch;
            totalQtyExpired   += qtyExpired;
        }

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
                .build();
    }
}