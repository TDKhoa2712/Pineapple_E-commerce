package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.CreateFarmRequest;
import backend.pineapple_ecommerce.dto.response.FarmResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.dto.response.UploadResponse;
import backend.pineapple_ecommerce.enums.FarmStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Quản lý trang trại (nguồn gốc sản phẩm).
 * Farmer chỉ quản lý farm của chính mình; Admin có toàn quyền.
 *
 * Workflow duyệt farm:
 * PENDING_APPROVAL → ACTIVE (approve) hoặc REJECTED (reject)
 */
public interface FarmService {

    FarmResponse createFarm(Long ownerId, CreateFarmRequest request);

    FarmResponse getFarmById(Long farmId);

    /** Public listing — chỉ ACTIVE farm */
    PageResponse<FarmResponse> getAllFarms(int page, int size);

    /** Admin listing — tất cả status, có thể filter */
    PageResponse<FarmResponse> getAllFarmsAdmin(int page, int size, FarmStatus status);

    List<FarmResponse> getMyFarms(Long ownerId);

    FarmResponse updateFarm(Long farmId, Long requesterId, CreateFarmRequest request);

    /** Soft delete — không xoá dữ liệu thật */
    void deleteFarm(Long farmId, Long requesterId);

    // ─────────────────────────────────────────────
    // NEW — 2.5
    // ─────────────────────────────────────────────

    /** Admin duyệt farm: PENDING_APPROVAL → ACTIVE */
    FarmResponse approveFarm(Long farmId);

    /** Admin từ chối farm: PENDING_APPROVAL → REJECTED, lưu lý do */
    FarmResponse rejectFarm(Long farmId, String reason);

    /** Upload/thay ảnh farm qua Cloudinary */
    FarmResponse uploadFarmImage(Long farmId, Long requesterId, MultipartFile image);

    /** Lấy sản phẩm của farm (qua InventoryBatch) */
    PageResponse<ProductSummaryResponse> getFarmProducts(Long farmId, int page, int size);
}
