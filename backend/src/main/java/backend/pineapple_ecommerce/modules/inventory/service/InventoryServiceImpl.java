package backend.pineapple_ecommerce.modules.inventory.service;

import backend.pineapple_ecommerce.common.enums.BatchStatus;
import backend.pineapple_ecommerce.modules.inventory.mapper.InventoryBatchMapper;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import backend.pineapple_ecommerce.modules.inventory.models.StockAdjustment;
import backend.pineapple_ecommerce.modules.inventory.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.modules.inventory.repository.StockAdjustmentRepository;
import backend.pineapple_ecommerce.modules.inventory.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.modules.inventory.dto.request.StockAdjustmentRequest;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventoryBatchResponse;
import backend.pineapple_ecommerce.modules.inventory.dto.response.InventorySummaryResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.inventory.dto.response.StockAdjustmentResponse;
import backend.pineapple_ecommerce.modules.farm.models.Farm;
import backend.pineapple_ecommerce.modules.order.models.OrderItem;
import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.modules.farm.repository.FarmRepository;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import backend.pineapple_ecommerce.modules.inventory.event.ProductStockChangedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryBatchRepository inventoryBatchRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductRepository         productRepository;
    private final FarmRepository            farmRepository;
    private final UserRepository            userRepository;
    private final InventoryBatchMapper inventoryBatchMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────
    // Batch management (existing)
    // ─────────────────────────────────────────────

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
        eventPublisher.publishEvent(new ProductStockChangedEvent(this, product.getId()));
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
        InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(batchId)
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
        eventPublisher.publishEvent(new ProductStockChangedEvent(this, batch.getProduct().getId()));
        return StockAdjustmentResponse.builder()
                .id(saved.getId()).batchId(batch.getId()).batchCode(batch.getBatchCode())
                .productId(batch.getProduct().getId()).productName(batch.getProduct().getName())
                .adjustmentQty(request.getAdjustmentQty()).reason(request.getReason())
                .qtyBefore(qtyBefore).qtyAfter(qtyAfter)
                .adjustedByName(admin.getFullName()).createdAt(saved.getCreatedAt()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentResponse> getBatchAdjustments(Long batchId) {
        if (!inventoryBatchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("InventoryBatch", batchId);
        }
        return stockAdjustmentRepository.findByBatchIdOrderByCreatedAtDesc(batchId).stream()
                .map(adj -> StockAdjustmentResponse.builder()
                        .id(adj.getId())
                        .batchId(adj.getBatch().getId())
                        .batchCode(adj.getBatch().getBatchCode())
                        .productId(adj.getBatch().getProduct().getId())
                        .productName(adj.getBatch().getProduct().getName())
                        .adjustmentQty(adj.getAdjustmentQty())
                        .reason(adj.getReason())
                        .qtyBefore(adj.getQtyBefore())
                        .qtyAfter(adj.getQtyAfter())
                        .adjustedByName(adj.getAdjustedBy() != null ? adj.getAdjustedBy().getFullName() : "N/A")
                        .createdAt(adj.getCreatedAt())
                        .build())
                .toList();
    }

    // ─────────────────────────────────────────────
    // Order-domain operations
    // ─────────────────────────────────────────────

    /**
     * Trừ tồn kho FIFO với pessimistic lock, ném BusinessException nếu không đủ hàng.
     * Được gọi từ OrderServiceImpl bên trong @Transactional — không cần @Transactional riêng.
     */
    @Override
    @Transactional
    public List<InventoryService.BatchAllocation> deductStockFifo(Long productId, int quantity) {
        List<InventoryBatch> batches = inventoryBatchRepository
                .findByProductIdAndStatusWithLock(productId, BatchStatus.AVAILABLE);

        int totalStock = batches.stream().mapToInt(InventoryBatch::getRemainingQuantity).sum();
        if (totalStock < quantity) {
            // Lấy tên sản phẩm từ batch đầu tiên (đã join fetch)
            String productName = batches.isEmpty()
                    ? "productId=" + productId
                    : batches.get(0).getProduct().getName();
            throw new BusinessException(
                    String.format("Sản phẩm '%s' chỉ còn %d trong kho", productName, totalStock));
        }

        List<InventoryService.BatchAllocation> allocations = new ArrayList<>();
        int remaining = quantity;

        for (InventoryBatch batch : batches) {
            if (remaining <= 0) break;
            int deduct = Math.min(batch.getRemainingQuantity(), remaining);
            batch.deductStock(deduct);
            inventoryBatchRepository.save(batch);
            allocations.add(new InventoryService.BatchAllocation(batch, deduct));
            remaining -= deduct;
        }

        eventPublisher.publishEvent(new ProductStockChangedEvent(this, productId));
        return allocations;
    }

    /**
     * Hoàn lại tồn kho cho tất cả OrderItem có batch.
     * Được gọi từ OrderServiceImpl bên trong @Transactional — không cần @Transactional riêng.
     */
    public void restoreStockForOrder(List<OrderItem> orderItems) {
        java.util.Set<Long> uniqueProductIds = new java.util.HashSet<>();
        for (OrderItem item : orderItems) {
            if (item.getBatch() == null) continue;
            InventoryBatch batch = inventoryBatchRepository
                    .findByIdWithLock(item.getBatch().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryBatch", item.getBatch().getId()));
            batch.setRemainingQuantity(batch.getRemainingQuantity() + item.getQuantity());
            if (batch.getStatus() == BatchStatus.SOLD_OUT) batch.setStatus(BatchStatus.AVAILABLE);
            inventoryBatchRepository.save(batch);
            if (batch.getProduct() != null) {
                uniqueProductIds.add(batch.getProduct().getId());
            }
        }
        for (Long productId : uniqueProductIds) {
            eventPublisher.publishEvent(new ProductStockChangedEvent(this, productId));
        }
    }

    // ─────────────────────────────────────────────
    // Farm-domain query (new)
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Long> getDistinctProductIdsByFarm(Long farmId) {
        return inventoryBatchRepository.findDistinctProductIdsByFarmId(farmId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryBatchResponse> getAllBatches(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InventoryBatch> rawPage = inventoryBatchRepository.findAllWithProductAndFarm(pageable);
        List<InventoryBatchResponse> content = rawPage.getContent().stream()
                .map(inventoryBatchMapper::toResponse)
                .toList();
        return PageResponse.of(new PageImpl<>(content, pageable, rawPage.getTotalElements()));
    }
}