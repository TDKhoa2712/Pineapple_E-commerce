package backend.pineapple_ecommerce.modules.farm.service;

import backend.pineapple_ecommerce.modules.farm.models.Farm;
import backend.pineapple_ecommerce.modules.farm.repository.FarmRepository;
import backend.pineapple_ecommerce.common.enums.FarmStatus;
import backend.pineapple_ecommerce.modules.farm.mapper.FarmMapper;
import backend.pineapple_ecommerce.modules.farm.dto.request.CreateFarmRequest;
import backend.pineapple_ecommerce.modules.farm.dto.response.FarmResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.product.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import backend.pineapple_ecommerce.infrastructure.cloudinary.CloudinaryService;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.enums.UploadFolder;
import backend.pineapple_ecommerce.event.EmailEvents;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import backend.pineapple_ecommerce.modules.inventory.service.InventoryService;
import backend.pineapple_ecommerce.modules.product.service.ProductService;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import backend.pineapple_ecommerce.common.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmServiceImpl implements FarmService {

    private final FarmRepository farmRepository;
    private final FarmMapper farmMapper;
    private final CloudinaryService cloudinaryService;
    private final UserService userService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileValidator fileValidator;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public FarmResponse createFarm(Long ownerId, CreateFarmRequest request) {
        User owner = userService.getEntityUser(ownerId);

        Farm farm = farmMapper.toEntity(request);
        farm.setOwner(owner);
        // status mặc định là PENDING_APPROVAL (set trong entity)

        Farm saved = farmRepository.save(farm);
        log.info("Farm created: id={}, owner={}, status=PENDING_APPROVAL", saved.getId(), ownerId);
        return farmMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public FarmResponse getFarmById(Long farmId) {
        Farm farm = farmRepository.findByIdAndIsDeletedFalse(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmId));
        return farmMapper.toResponse(farm);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FarmResponse> getAllFarms(int page, int size) {
        // Public: chỉ ACTIVE farm
        var result = farmRepository
                .findByStatusAndIsDeletedFalse(FarmStatus.ACTIVE,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(farmMapper::toResponse);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FarmResponse> getAllFarmsAdmin(int page, int size, FarmStatus status) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = (status != null)
                ? farmRepository.findByStatusAndIsDeletedFalse(status, pageable)
                : farmRepository.findByIsDeletedFalse(pageable);
        return PageResponse.of(result.map(farmMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmResponse> getMyFarms(Long ownerId) {
        return farmRepository.findByOwnerIdAndIsDeletedFalse(ownerId).stream()
                .map(farmMapper::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public FarmResponse updateFarm(Long farmId, Long requesterId, CreateFarmRequest request) {
        Farm farm = findActiveById(farmId);
        verifyOwnership(farm, requesterId);

        farmMapper.updateFromRequest(request, farm);
        Farm saved = farmRepository.save(farm);
        log.info("Farm updated: id={}", farmId);
        return farmMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // DELETE — Soft delete
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteFarm(Long farmId, Long requesterId) {
        Farm farm = findActiveById(farmId);
        verifyOwnership(farm, requesterId);

        farm.setIsDeleted(true);  // Soft delete — giữ lại dữ liệu cho audit
        farmRepository.save(farm);
        log.info("Farm soft-deleted: id={}, by userId={}", farmId, requesterId);
    }

    // ─────────────────────────────────────────────
    // NEW — 2.5: APPROVAL WORKFLOW
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public FarmResponse approveFarm(Long farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmId));

        if (farm.getStatus() != FarmStatus.PENDING_APPROVAL) {
            throw new BusinessException(
                    "Chỉ có thể duyệt farm ở trạng thái PENDING_APPROVAL. " +
                            "Trạng thái hiện tại: " + farm.getStatus());
        }

        farm.setStatus(FarmStatus.ACTIVE);
        Farm saved = farmRepository.save(farm);
        log.info("Farm approved: id={}", farmId);

        // Publish event — email gửi sau COMMIT
        eventPublisher.publishEvent(new EmailEvents.FarmApprovalEvent(
                saved.getOwner().getEmail(),
                saved.getName(),
                true,
                null));

        return farmMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FarmResponse rejectFarm(Long farmId, String reason) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmId));

        if (farm.getStatus() != FarmStatus.PENDING_APPROVAL) {
            throw new BusinessException(
                    "Chỉ có thể từ chối farm ở trạng thái PENDING_APPROVAL. " +
                            "Trạng thái hiện tại: " + farm.getStatus());
        }

        farm.setStatus(FarmStatus.REJECTED);
        farm.setRejectionReason(reason);
        Farm saved = farmRepository.save(farm);
        log.info("Farm rejected: id={}, reason={}", farmId, reason);

        // Publish event — email gửi sau COMMIT
        eventPublisher.publishEvent(new EmailEvents.FarmApprovalEvent(
                saved.getOwner().getEmail(),
                saved.getName(),
                false,
                reason));

        return farmMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // NEW — 2.5: UPLOAD IMAGE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public FarmResponse uploadFarmImage(Long farmId, Long requesterId, MultipartFile image) {
        Farm farm = findActiveById(farmId);
        verifyOwnership(farm, requesterId);

        // Validate file upload
        fileValidator.validateImage(image);

        String oldPublicId = farm.getImagePublicId();

        // Upload ảnh mới
        UploadResponse uploaded = cloudinaryService.uploadImage(image, UploadFolder.FARM);

        // Cập nhật thông tin ảnh mới
        farm.setImageUrl(uploaded.getUrl());
        farm.setImagePublicId(uploaded.getPublicId());

        Farm saved = farmRepository.save(farm);

        // Xóa ảnh cũ trên Cloudinary (nếu có)
        if (oldPublicId != null && !oldPublicId.isBlank()) {
            cloudinaryService.deleteImage(oldPublicId);
        }

        log.info("Farm image uploaded successfully: farmId={}, publicId={}", farmId, uploaded.getPublicId());
        return farmMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // NEW — 2.5: GET FARM PRODUCTS
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getFarmProducts(Long farmId, int page, int size) {
        // Kiểm tra farm tồn tại
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", farmId);
        }

        // Lấy distinct productIds từ batch của farm
        List<Long> productIds = inventoryService.getDistinctProductIdsByFarm(farmId);

        if (productIds.isEmpty()) {
            return PageResponse.<ProductSummaryResponse>builder()
                    .content(List.of())
                    .page(page).size(size)
                    .totalElements(0).totalPages(0).last(true)
                    .build();
        }

        // Query products theo IDs với phân trang
        List<ProductSummaryResponse> allProducts = productService.getProductsByIds(productIds);

        int start = page * size;
        int end   = Math.min(start + size, allProducts.size());

        List<ProductSummaryResponse> content = (start >= allProducts.size())
                ? List.of()
                : allProducts.subList(start, end);

        return PageResponse.of(new PageImpl<>(content, PageRequest.of(page, size), allProducts.size()));
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Farm findActiveById(Long farmId) {
        return farmRepository.findByIdAndIsDeletedFalse(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmId));
    }

    private void verifyOwnership(Farm farm, Long requesterId) {
        if (!farm.getOwner().getId().equals(requesterId)) {
            throw new UnauthorizedException("Bạn không có quyền thao tác trang trại này");
        }
    }
}
