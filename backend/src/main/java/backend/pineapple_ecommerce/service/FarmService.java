package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.CreateFarmRequest;
import backend.pineapple_ecommerce.dto.response.FarmResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;

import java.util.List;

/**
 * Quản lý trang trại (nguồn gốc sản phẩm).
 * Farmer chỉ quản lý farm của chính mình; Admin có toàn quyền.
 */
public interface FarmService {

    /** Tạo trang trại mới. Owner = user đang đăng nhập. */
    FarmResponse createFarm(Long ownerId, CreateFarmRequest request);

    /** Lấy chi tiết trang trại. */
    FarmResponse getFarmById(Long farmId);

    /** Danh sách tất cả trang trại — phân trang (Admin). */
    PageResponse<FarmResponse> getAllFarms(int page, int size);

    /** Danh sách trang trại của một farmer. */
    List<FarmResponse> getMyFarms(Long ownerId);

    /** Cập nhật thông tin trang trại. Chỉ chủ trang trại hoặc Admin. */
    FarmResponse updateFarm(Long farmId, Long requesterId, CreateFarmRequest request);

    /** Xoá trang trại. Chỉ chủ trang trại hoặc Admin. */
    void deleteFarm(Long farmId, Long requesterId);
}
