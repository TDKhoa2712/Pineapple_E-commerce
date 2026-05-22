package backend.pineapple_ecommerce.modules.category.service;

import backend.pineapple_ecommerce.modules.category.mapper.CategoryMapper;
import backend.pineapple_ecommerce.modules.category.models.Category;
import backend.pineapple_ecommerce.modules.category.repository.CategoryRepository;
import backend.pineapple_ecommerce.modules.category.dto.request.CreateCategoryRequest;
import backend.pineapple_ecommerce.modules.category.dto.response.CategoryResponse;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Sinh và validate slug
        String slug = resolveUniqueSlug(request.getSlug(), request.getName(), null);

        Category category = categoryMapper.toEntity(request);
        category.setSlug(slug);

        // Gán danh mục cha
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category (parent)", request.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, slug={}", saved.getId(), saved.getSlug());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        // Query lấy root + children trong 1 lần join fetch
        List<Category> roots = categoryRepository.findAllRootWithChildren();
        return roots.stream()
                .map(categoryMapper::toResponseWithChildren)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryMapper.toResponseList(categoryRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest request) {
        Category category = findById(id);

        // Nếu tên thay đổi và không cung cấp slug mới → tái sinh slug
        String newSlug = resolveUniqueSlug(request.getSlug(), request.getName(), category.getSlug());
        category.setSlug(newSlug);

        // Cập nhật parent (cho phép set null để đưa về root)
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException("Danh mục không thể là cha của chính nó");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category (parent)", request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        categoryMapper.updateFromRequest(request, category);
        Category saved = categoryRepository.save(category);
        log.info("Category updated: id={}", saved.getId());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findById(id);

        // Không cho xoá nếu còn sản phẩm
        if (!category.getProducts().isEmpty()) {
            throw new BusinessException("Không thể xoá danh mục đang có sản phẩm");
        }
        // Không cho xoá nếu còn danh mục con
        if (!category.getChildren().isEmpty()) {
            throw new BusinessException("Không thể xoá danh mục đang có danh mục con");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: id={}", id);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    /**
     * Trả về slug unique.
     * - providedSlug: ưu tiên dùng nếu không rỗng.
     * - name: dùng để gen slug nếu providedSlug rỗng.
     * - currentSlug: bỏ qua check trùng nếu slug không đổi (update case).
     */
    private String resolveUniqueSlug(String providedSlug, String name, String currentSlug) {
        String base = (providedSlug != null && !providedSlug.isBlank())
                ? SlugUtils.toSlug(providedSlug)
                : SlugUtils.toSlug(name);

        // Không đổi slug thì giữ nguyên
        if (base.equals(currentSlug)) return base;

        if (!categoryRepository.existsBySlug(base)) return base;

        int suffix = 1;
        String candidate;
        do {
            candidate = base + "-" + suffix++;
        } while (categoryRepository.existsBySlug(candidate));

        return candidate;
    }
}
