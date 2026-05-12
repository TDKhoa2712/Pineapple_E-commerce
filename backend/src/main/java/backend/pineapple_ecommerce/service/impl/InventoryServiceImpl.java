package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.CreateInventoryBatchRequest;
import backend.pineapple_ecommerce.dto.response.InventoryBatchResponse;
import backend.pineapple_ecommerce.entity.Farm;
import backend.pineapple_ecommerce.entity.InventoryBatch;
import backend.pineapple_ecommerce.entity.Product;
import backend.pineapple_ecommerce.enums.BatchStatus;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.mapper.InventoryBatchMapper;
import backend.pineapple_ecommerce.repository.FarmRepository;
import backend.pineapple_ecommerce.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.repository.ProductRepository;
import backend.pineapple_ecommerce.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryBatchRepository inventoryBatchRepository;
    private final ProductRepository        productRepository;
    private final FarmRepository           farmRepository;
    private final InventoryBatchMapper     inventoryBatchMapper;

    @Override
    @Transactional
    public InventoryBatchResponse addBatch(CreateInventoryBatchRequest request) {
        // Validate batchCode unique
        if (inventoryBatchRepository.findByBatchCode(request.getBatchCode()).isPresent()) {
            throw new BusinessException("Mã lô đã tồn tại: " + request.getBatchCode());
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        Farm farm = null;
        if (request.getFarmId() != null) {
            farm = farmRepository.findById(request.getFarmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm", request.getFarmId()));
        }

        // Validate ngày hết hạn > ngày thu hoạch
        if (request.getHarvestDate() != null && request.getExpiryDate() != null
                && !request.getExpiryDate().isAfter(request.getHarvestDate())) {
            throw new BusinessException("Ngày hết hạn phải sau ngày thu hoạch");
        }

        InventoryBatch batch = inventoryBatchMapper.toEntity(request);
        batch.setProduct(product);
        batch.setFarm(farm);
        batch.setRemainingQuantity(request.getQuantity()); // khởi tạo bằng quantity

        InventoryBatch saved = inventoryBatchRepository.save(batch);
        log.info("Inventory batch added: batchCode={}, productId={}, qty={}",
                saved.getBatchCode(), product.getId(), saved.getQuantity());
        return inventoryBatchMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getAvailableBatches(Long productId) {
        return inventoryBatchRepository
                .findByProductIdAndStatus(productId, BatchStatus.AVAILABLE)
                .stream()
                .map(inventoryBatchMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getAllBatchesByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return inventoryBatchRepository
                .findByProductIdAndStatus(productId, BatchStatus.AVAILABLE) // mở rộng thêm filter nếu cần
                .stream()
                .map(inventoryBatchMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryBatchResponse getBatchById(Long batchId) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryBatch", batchId));
        return inventoryBatchMapper.toResponse(batch);
    }

    /**
     * Tự động quét và đánh dấu các lô hàng quá hạn.
     * Chạy mỗi ngày lúc 01:00 SA để không ảnh hưởng giờ cao điểm.
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void markExpiredBatches() {
        LocalDate today = LocalDate.now();
        List<InventoryBatch> expiredBatches = inventoryBatchRepository
                .findByStatus(BatchStatus.AVAILABLE)   // FIX: dùng findByStatus thay vì findByProductIdAndStatus(0L, ...)
                .stream()
                .filter(b -> b.getExpiryDate() != null && b.getExpiryDate().isBefore(today))
                .toList();

        expiredBatches.forEach(b -> {
            b.setStatus(BatchStatus.EXPIRED);
            inventoryBatchRepository.save(b);
        });

        if (!expiredBatches.isEmpty()) {
            log.warn("Marked {} batches as EXPIRED", expiredBatches.size());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalStock(Long productId) {
        Integer stock = inventoryBatchRepository.getTotalAvailableStock(productId);
        return stock != null ? stock : 0;
    }
}
