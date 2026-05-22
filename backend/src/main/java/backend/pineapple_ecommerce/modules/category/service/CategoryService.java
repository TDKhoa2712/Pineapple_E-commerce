package backend.pineapple_ecommerce.modules.category.service;

import backend.pineapple_ecommerce.modules.category.dto.request.CreateCategoryRequest;
import backend.pineapple_ecommerce.modules.category.dto.response.CategoryResponse;

import java.util.List;

/**
 * Quản lý cây danh mục (parent / children).
 * Hỗ trợ cả cấu trúc phẳng (flat list) và cây lồng nhau (nested tree).
 */
public interface CategoryService {

    /** Tạo danh mục mới. Tự gen slug; nếu có parentId thì gán danh mục cha. */
    CategoryResponse createCategory(CreateCategoryRequest request);

    /** Lấy toàn bộ danh mục gốc kèm children (1 cấp) — dùng cho nav/menu. */
    List<CategoryResponse> getCategoryTree();

    /** Lấy danh sách phẳng tất cả danh mục — dùng cho dropdown Admin. */
    List<CategoryResponse> getAllCategories();

    /** Lấy chi tiết danh mục theo id. */
    CategoryResponse getCategoryById(Long id);

    /** Lấy chi tiết danh mục theo slug. */
    CategoryResponse getCategoryBySlug(String slug);

    /** Cập nhật danh mục (name, image, parentId). */
    CategoryResponse updateCategory(Long id, CreateCategoryRequest request);

    /**
     * Xoá danh mục.
     * Ném BusinessException nếu danh mục còn sản phẩm hoặc danh mục con.
     */
    void deleteCategory(Long id);
}
