package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.CreateCategoryRequest;
import backend.pineapple_ecommerce.dto.response.CategoryResponse;
import backend.pineapple_ecommerce.entity.Category;
import backend.pineapple_ecommerce.entity.Product;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.mapper.CategoryMapper;
import backend.pineapple_ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl")
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper      categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    // ── Fixtures ──────────────────────────────────────────────────────

    private static final Long CATEGORY_ID = 1L;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(CATEGORY_ID)
                .name("Trái cây nhiệt đới")
                .slug("trai-cay-nhiet-doi")
                .products(new ArrayList<>())
                .children(new ArrayList<>())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // createCategory
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCategory()")
    class CreateCategory {

        private CreateCategoryRequest buildRequest(String name, String slug, Long parentId) {
            CreateCategoryRequest req = new CreateCategoryRequest();
            req.setName(name);
            req.setSlug(slug);
            req.setParentId(parentId);
            return req;
        }

        @Test
        @DisplayName("tạo danh mục gốc thành công → trả về CategoryResponse")
        void givenRootCategory_shouldReturnResponse() {
            CreateCategoryRequest req = buildRequest("Rau củ", null, null);
            CategoryResponse expected = CategoryResponse.builder().build();

            when(categoryRepository.existsBySlug("rau-cu")).thenReturn(false);
            when(categoryMapper.toEntity(req)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(expected);

            CategoryResponse result = categoryService.createCategory(req);

            assertThat(result).isSameAs(expected);
            assertThat(category.getSlug()).isEqualTo("rau-cu");
        }

        @Test
        @DisplayName("slug trùng → tự động thêm hậu tố '-1'")
        void givenDuplicateSlug_shouldAddSuffix() {
            CreateCategoryRequest req = buildRequest("Rau củ", null, null);

            when(categoryRepository.existsBySlug("rau-cu")).thenReturn(true);
            when(categoryRepository.existsBySlug("rau-cu-1")).thenReturn(false);
            when(categoryMapper.toEntity(req)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().build());

            categoryService.createCategory(req);

            assertThat(category.getSlug()).isEqualTo("rau-cu-1");
        }

        @Test
        @DisplayName("có parentId hợp lệ → gán parent cho category")
        void givenValidParentId_shouldSetParent() {
            Long parentId = 99L;
            Category parent = Category.builder().id(parentId).name("Nông sản").build();
            CreateCategoryRequest req = buildRequest("Rau lá", null, parentId);

            when(categoryRepository.existsBySlug(any())).thenReturn(false);
            when(categoryMapper.toEntity(req)).thenReturn(category);
            when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().build());

            categoryService.createCategory(req);

            assertThat(category.getParent()).isEqualTo(parent);
        }

        @Test
        @DisplayName("parentId không tồn tại → ném ResourceNotFoundException")
        void givenInvalidParentId_shouldThrow() {
            CreateCategoryRequest req = buildRequest("Sub", null, 999L);

            when(categoryRepository.existsBySlug(any())).thenReturn(false);
            when(categoryMapper.toEntity(req)).thenReturn(category);
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.createCategory(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("providedSlug được ưu tiên hơn name")
        void givenProvidedSlug_shouldUseItInsteadOfName() {
            CreateCategoryRequest req = buildRequest("Trái cây", "custom-slug", null);

            when(categoryRepository.existsBySlug("custom-slug")).thenReturn(false);
            when(categoryMapper.toEntity(req)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().build());

            categoryService.createCategory(req);

            assertThat(category.getSlug()).isEqualTo("custom-slug");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // updateCategory
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCategory()")
    class UpdateCategory {

        @Test
        @DisplayName("cập nhật tên → slug được tái sinh")
        void givenNameChange_shouldRegenerateSlug() {
            CreateCategoryRequest req = new CreateCategoryRequest();
            req.setName("Trái cây nhiệt đới mới");

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.existsBySlug("trai-cay-nhiet-doi-moi")).thenReturn(false);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().build());

            categoryService.updateCategory(CATEGORY_ID, req);

            assertThat(category.getSlug()).isEqualTo("trai-cay-nhiet-doi-moi");
        }

        @Test
        @DisplayName("slug không đổi → không tái sinh (update idempotent)")
        void givenSameSlug_shouldKeepCurrentSlug() {
            CreateCategoryRequest req = new CreateCategoryRequest();
            req.setName("Trái cây nhiệt đới");
            // slug sẽ gen ra "trai-cay-nhiet-doi" trùng với current → giữ nguyên

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().build());

            categoryService.updateCategory(CATEGORY_ID, req);

            assertThat(category.getSlug()).isEqualTo("trai-cay-nhiet-doi");
            // existsBySlug không được gọi vì slug không đổi
            verify(categoryRepository, never()).existsBySlug(any());
        }

        @Test
        @DisplayName("parentId = chính nó → ném BusinessException")
        void givenSelfAsParent_shouldThrow() {
            CreateCategoryRequest req = new CreateCategoryRequest();
            req.setName("New Name");
            req.setParentId(CATEGORY_ID); // tự đặt chính mình làm cha

            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.existsBySlug(any())).thenReturn(false);

            assertThatThrownBy(() -> categoryService.updateCategory(CATEGORY_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cha của chính nó");
        }

        @Test
        @DisplayName("category không tồn tại → ném ResourceNotFoundException")
        void givenMissingCategory_shouldThrow() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(CATEGORY_ID, new CreateCategoryRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // deleteCategory
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCategory()")
    class DeleteCategory {

        @Test
        @DisplayName("category rỗng → xoá thành công")
        void givenEmptyCategory_shouldDelete() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            categoryService.deleteCategory(CATEGORY_ID);

            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("còn sản phẩm → ném BusinessException")
        void givenCategoryWithProducts_shouldThrow() {
            category.getProducts().add(new Product());
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("sản phẩm");
        }

        @Test
        @DisplayName("còn danh mục con → ném BusinessException")
        void givenCategoryWithChildren_shouldThrow() {
            category.getChildren().add(new Category());
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("danh mục con");
        }

        @Test
        @DisplayName("category không tồn tại → ném ResourceNotFoundException")
        void givenMissingCategory_shouldThrow() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // getCategoryTree / getAllCategories
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCategoryTree() & getAllCategories()")
    class Query {

        @Test
        @DisplayName("getCategoryTree → map toResponseWithChildren")
        void getCategoryTree_shouldReturnMappedTree() {
            CategoryResponse resp = CategoryResponse.builder().build();
            when(categoryRepository.findAllRootWithChildren()).thenReturn(List.of(category));
            when(categoryMapper.toResponseWithChildren(category)).thenReturn(resp);

            List<CategoryResponse> result = categoryService.getCategoryTree();

            assertThat(result).containsExactly(resp);
        }

        @Test
        @DisplayName("getAllCategories → map toResponseList")
        void getAllCategories_shouldReturnFlatList() {
            when(categoryRepository.findAll()).thenReturn(List.of(category));
            when(categoryMapper.toResponseList(any())).thenReturn(List.of(CategoryResponse.builder().build()));

            List<CategoryResponse> result = categoryService.getAllCategories();

            assertThat(result).hasSize(1);
        }
    }
}