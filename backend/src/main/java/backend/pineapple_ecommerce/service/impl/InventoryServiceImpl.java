package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.dto.request.StockAdjustmentRequest;
import backend.pineapple_ecommerce.dto.response.InventoryBatchResponse;
import backend.pineapple_ecommerce.dto.response.InventorySummaryResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.StockAdjustmentResponse;
import backend.pineapple_ecommerce.entity.Farm;
import backend.pineapple_ecommerce.entity.InventoryBatch;
import backend.pineapple_ecommerce.entity.Product;
import backend.pineapple_ecommerce.entity.StockAdjustment;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.enums.BatchStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.mapper.InventoryBatchMapper;
import backend.pineapple_ecommerce.repository.FarmRepository;
import backend.pineapple_ecommerce.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.repository.ProductRepository;
import backend.pineapple_ecommerce.repository.StockAdjustmentRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryBatchRepository  inventoryBatchRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductRepository         productRepository;
    private final FarmRepository            farmRepository;
    private final UserRepository            userRepository;
    private final InventoryBatchMapper      inventoryBatchMapper;

    @Override
    @Transactional
    public InventoryBatchResponse addBatch(CreateInventoryBatchRequest request) {
        if (inventoryBatchRepository.findByBatchCode(request.getBatchCode()).isPresent()) {
            throw new BusinessException("Ma lo da ton tai: " + request.getBatchCode());
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));
        Farm farm = null;
        if (request.getFarmId() != null) {
            farm = farmRepository.findById(request.getFarmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm", request.getFarmId()));
        }
        if (request.getHarvestDate() != null && request.getExpiryDate() != null
                && !request.getExpiryDate().isAfter(request.getHarvestDate())) {
            throw new BusinessException("Ngay het han phai sau ngay thu hoach");
        }
        InventoryBatch batch = inventoryBatchMapper.toEntity(request);
        batch.setProduct(product);
        batch.setFarm(farm);
        batch.setRemainingQuantity(request.getQuantity());
        InventoryBatch saved = inventoryBatchRepository.save(batch);
        log.info("Batch added: batchCode={}, productId={}, qty={}", saved.getBatchCode(), product.getId(), saved.getQuantity());
        return inventoryBatchMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getAvailableBatches(Long productId) {
        return inventoryBatchRepository.findByProductIdAndStatus(productId, BatchStatus.AVAILABLE)
                .stream().map(inventoryBatchMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getAllBatchesByProduct(Long productId) {
        if (!productRepository.existsById(productId)) throw new ResourceNotFoundException("Product", productId);
        return inventoryBatchRepository.findByProductIdAndStatus(productId, BatchStatus.AVAILABLE)
                .stream().map(inventoryBatchMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryBatchResponse getBatchById(Long batchId) {
        return inventoryBatchMapper.toResponse(inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryBatch", batchId)));
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void markExpiredBatches() {
        int count = doMarkExpired();
        if (count > 0) log.warn("Scheduled: marked {} batches as EXPIRED", count);
    }

    @Override
    @Transactional
    public int markExpiredBatchesManual() {
        int count = doMarkExpired();
        log.info("Manual mark-expired: {} batches marked", count);
        return count;
    }

    private int doMarkExpired() {
        LocalDate today = LocalDate.now();
        List<InventoryBatch> expired = inventoryBatchRepository.findByStatus(BatchStatus.AVAILABLE)
                .stream().filter(b -> b.getExpiryDate() != null && b.getExpiryDate().isBefore(today)).toList();
        expired.forEach(b -> { b.setStatus(BatchStatus.EXPIRED); inventoryBatchRepository.save(b); });
        return expired.size();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalStock(Long productId) {
        Integer stock = inventoryBatchRepository.getTotalAvailableStock(productId);
        return stock != null ? stock : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getExpiringSoon(int days) {
        if (days < 1 || days > 365) throw new BusinessException("So ngay phai trong khoang 1-365");
        LocalDate threshold = LocalDate.now().plusDays(days);
        return inventoryBatchRepository.findExpiringSoon(threshold).stream()
                .map(inventoryBatchMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventorySummaryResponse> getInventorySummary(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "totalStock"));
        Page<Object[]> rawPage = inventoryBatchRepository.findInventorySummaryRaw(pageable);
        List<InventorySummaryResponse> content = rawPage.getContent().stream()
                .map(row -> InventorySummaryResponse.builder()
                        .productId(((Number) row[0]).longValue())
                        .productName((String) row[1])
                        .totalStock(((Number) row[2]).intValue())
                        .batchCount(((Number) row[3]).longValue())
                        .build())
                .toList();
        return PageResponse.of(new PageImpl<>(content, pageable, rawPage.getTotalElements()));
    }

    @Override
    @Transactional
    public StockAdjustmentResponse adjustBatch(Long batchId, Long adminUserId, StockAdjustmentRequest request) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryBatch", batchId));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId));

        int qtyBefore = batch.getRemainingQuantity();
        int qtyAfter  = qtyBefore + request.getAdjustmentQty();

        if (qtyAfter < 0) {
            throw new BusinessException(String.format(
                "Ton kho hien tai %d, dieu chinh %d se am", qtyBefore, request.getAdjustmentQty()));
        }

        batch.setRemainingQuantity(qtyAfter);
        if (qtyAfter == 0) batch.setStatus(BatchStatus.SOLD_OUT);
        else if (batch.getStatus() == BatchStatus.SOLD_OUT) batch.setStatus(BatchStatus.AVAILABLE);
        inventoryBatchRepository.save(batch);

        StockAdjustment adj = StockAdjustment.builder()
                .batch(batch).adjustmentQty(request.getAdjustmentQty())
                .reason(request.getReason()).adjustedBy(admin)
                .qtyBefore(qtyBefore).qtyAfter(qtyAfter).build();
        StockAdjustment saved = stockAdjustmentRepository.save(adj);

        log.info("Stock adjusted: batchId={}, by={}, {}->{}; reason={}", batchId, adminUserId, qtyBefore, qtyAfter, request.getReason());
        return StockAdjustmentResponse.builder()
                .id(saved.getId()).batchId(batch.getId()).batchCode(batch.getBatchCode())
                .productId(batch.getProduct().getId()).productName(batch.getProduct().getName())
                .adjustmentQty(request.getAdjustmentQty()).reason(request.getReason())
                .qtyBefore(qtyBefore).qtyAfter(qtyAfter)
                .adjustedByName(admin.getFullName()).createdAt(saved.getCreatedAt()).build();
    }
}
