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
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import backend.pineapple_ecommerce.common.enums.RoleName;
import backend.pineapple_ecommerce.common.enums.FarmStatus;
import backend.pineapple_ecommerce.modules.farm.repository.FarmRepository;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.modules.user.service.UserService;
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
    private final UserService               userService;

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

        Long currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getEntityUser(currentUserId);
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            if (farm == null) {
                throw new BusinessException("Farmer must specify a farm for the batch");
            }
            if (!farm.getOwner().getId().equals(currentUserId)) {
                throw new UnauthorizedException("Bạn không có quyền thêm lô hàng vào trang trại này");
            }
        }

        if (farm != null && farm.getStatus() == FarmStatus.INACTIVE) {
            throw new BusinessException("Trang trai dang ngung hoat dong nen khong the thao tac ton kho.");
        }

        if (request.getHarvestDate() != null && request.getExpiryDate() != null
                && !request.getExpiryDate().isAfter(request.getHarvestDate())) {
            throw new BusinessException("Ngay het han phai sau ngay thu hoach");
        }
        InventoryBatch batch = inventoryBatchMapper.toEntity(request);
        batch.setProduct(product);
        batch.setFarm(farm);
        // Luong nghiep vu:
        // - Admin nhap kho: duoc cong thang vao kho (AVAILABLE)
        // - Farmer nhap kho: tao lo PENDING_APPROVAL, chua cong vao kho
        if (isAdmin) {
            batch.setStatus(BatchStatus.AVAILABLE);
            batch.setRemainingQuantity(request.getQuantity());
            batch.setRejectionReason(null);
        } else {
            batch.setStatus(BatchStatus.PENDING_APPROVAL);
            batch.setRemainingQuantity(0);
            // rejectionReason (neu co) mac dinh null
            batch.setRejectionReason(null);
        }

        InventoryBatch saved = inventoryBatchRepository.save(batch);
        if (isAdmin) {
            eventPublisher.publishEvent(new ProductStockChangedEvent(this, product.getId()));
        }
        log.info(
                "Batch added: batchCode={}, productId={}, qty={}, status={}, remainingQty={}",
                saved.getBatchCode(), product.getId(), saved.getQuantity(),
                saved.getStatus(), saved.getRemainingQuantity());
        return inventoryBatchMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryBatchResponse approveBatch(Long batchId, Long adminUserId) {
        InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryBatch", batchId));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId));

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền duyệt lô hàng nhập");
        }

        if (batch.getStatus() != BatchStatus.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể duyệt lô ở trạng thái PENDING_APPROVAL. Trạng thái hiện tại: " + batch.getStatus());
        }

        if (batch.getFarm() != null && batch.getFarm().getStatus() == FarmStatus.INACTIVE) {
            throw new BusinessException("Trang trại đang ngừng hoạt động nên không thể duyệt/thêm tồn kho.");
        }

        batch.setStatus(BatchStatus.AVAILABLE);
        batch.setRemainingQuantity(batch.getQuantity());
        batch.setRejectionReason(null);
        InventoryBatch saved = inventoryBatchRepository.save(batch);

        eventPublisher.publishEvent(new ProductStockChangedEvent(this, saved.getProduct().getId()));
        return inventoryBatchMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryBatchResponse rejectBatch(Long batchId, Long adminUserId, String reason) {
        InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryBatch", batchId));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId));

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền từ chối lô hàng nhập");
        }

        if (batch.getStatus() != BatchStatus.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể từ chối lô ở trạng thái PENDING_APPROVAL. Trạng thái hiện tại: " + batch.getStatus());
        }

        batch.setStatus(BatchStatus.REJECTED);
        batch.setRemainingQuantity(0);
        batch.setRejectionReason(reason);
        InventoryBatch saved = inventoryBatchRepository.save(batch);

        log.info("Batch rejected: batchId={}, by={}, reason={}", batchId, adminUserId, reason);
        return inventoryBatchMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryBatchResponse resubmitBatch(Long batchId, Long farmerUserId) {
        InventoryBatch batch = inventoryBatchRepository.findByIdWithLock(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryBatch", batchId));

        if (batch.getStatus() != BatchStatus.REJECTED) {
            throw new BusinessException("Chỉ có thể gửi yêu cầu lại cho lô đang ở trạng thái REJECTED. Trạng thái hiện tại: " + batch.getStatus());
        }

        if (batch.getFarm() == null || batch.getFarm().getOwner() == null) {
            throw new BusinessException("Lô hàng này không thuộc quyền quản lý của farmer.");
        }

        if (!batch.getFarm().getOwner().getId().equals(farmerUserId)) {
            throw new UnauthorizedException("Bạn không có quyền gửi yêu cầu lại cho lô hàng này");
        }

        if (batch.getFarm().getStatus() == FarmStatus.INACTIVE) {
            throw new BusinessException("Trang trại đang ngừng hoạt động nên không thể gửi yêu cầu duyệt lô.");
        }

        batch.setStatus(BatchStatus.PENDING_APPROVAL);
        batch.setRemainingQuantity(0);
        // Giữ rejectionReason cho tới khi admin approve lần tiếp theo.
        InventoryBatch saved = inventoryBatchRepository.save(batch);
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
    public PageResponse<InventorySummaryResponse> getInventorySummary(String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String resolvedSortBy = "totalStock";
        if (sortBy != null && !sortBy.isBlank()) {
            resolvedSortBy = switch (sortBy) {
                case "productId", "id" -> "productId";
                case "productName", "name" -> "productName";
                case "batchCount" -> "batchCount";
                default -> "totalStock";
            };
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
        Page<Object[]> rawPage = inventoryBatchRepository.findInventorySummaryRaw(
                keyword != null ? keyword.trim() : "",
                pageable
        );
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

        boolean isAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            if (batch.getFarm() == null || !batch.getFarm().getOwner().getId().equals(adminUserId)) {
                throw new UnauthorizedException("Bạn không có quyền điều chỉnh lô hàng của trang trại này");
            }
        }

        if (batch.getFarm() != null && batch.getFarm().getStatus() == FarmStatus.INACTIVE) {
            throw new BusinessException("Trang trai dang ngung hoat dong nen khong the thao tac ton kho.");
        }

        if (batch.getStatus() != BatchStatus.AVAILABLE) {
            throw new BusinessException("Chỉ có thể điều chỉnh lô ở trạng thái AVAILABLE. Trạng thái hiện tại: " + batch.getStatus());
        }

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
    public PageResponse<InventoryBatchResponse> getFarmBatches(Long farmId, String keyword, int page, int size, String sortBy, String sortDirection) {
        Long currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getEntityUser(currentUserId);
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmId));

        if (!isAdmin && !farm.getOwner().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Bạn không có quyền xem lô hàng của trang trại này");
        }

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String resolvedSortBy = "createdAt";
        if (sortBy != null && !sortBy.isBlank()) {
            resolvedSortBy = switch (sortBy) {
                case "batchCode" -> "batchCode";
                case "quantity" -> "quantity";
                case "remainingQuantity" -> "remainingQuantity";
                case "harvestDate" -> "harvestDate";
                case "expiryDate" -> "expiryDate";
                case "status" -> "status";
                default -> "createdAt";
            };
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
        Page<InventoryBatch> rawPage = inventoryBatchRepository.findAllByFarmIdAndKeyword(
                farmId,
                keyword != null ? keyword.trim() : "",
                pageable
        );
        List<InventoryBatchResponse> content = rawPage.getContent().stream()
                .map(inventoryBatchMapper::toResponse)
                .toList();
        return PageResponse.of(new PageImpl<>(content, pageable, rawPage.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryBatchResponse> getAllBatches(String keyword, BatchStatus status, int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Map sort field to JPA entity field name
        String resolvedSortBy = "createdAt";
        if (sortBy != null && !sortBy.isBlank()) {
            resolvedSortBy = switch (sortBy) {
                case "batchCode" -> "batchCode";
                case "quantity" -> "quantity";
                case "remainingQuantity" -> "remainingQuantity";
                case "harvestDate" -> "harvestDate";
                case "expiryDate" -> "expiryDate";
                case "status" -> "status";
                default -> "createdAt";
            };
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
        Page<InventoryBatch> rawPage = inventoryBatchRepository.findAllWithProductAndFarm(
                keyword != null ? keyword.trim() : "",
                status,
                pageable
        );
        List<InventoryBatchResponse> content = rawPage.getContent().stream()
                .map(inventoryBatchMapper::toResponse)
                .toList();
        return PageResponse.of(new PageImpl<>(content, pageable, rawPage.getTotalElements()));
    }
}
