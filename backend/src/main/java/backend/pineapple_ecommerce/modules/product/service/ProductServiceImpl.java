package backend.pineapple_ecommerce.modules.product.service;

import backend.pineapple_ecommerce.modules.product.dto.response.ProductDetailResponse;
import backend.pineapple_ecommerce.modules.product.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import backend.pineapple_ecommerce.modules.product.mapper.ProductMapper;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
import backend.pineapple_ecommerce.common.enums.ProductStatus;
import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.product.models.ProductImage;
import backend.pineapple_ecommerce.modules.product.dto.request.CreateProductRequest;
import backend.pineapple_ecommerce.modules.product.dto.request.UpdateProductRequest;
import backend.pineapple_ecommerce.modules.category.models.Category;
import backend.pineapple_ecommerce.modules.category.repository.CategoryRepository;
import backend.pineapple_ecommerce.common.enums.UploadFolder;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.modules.inventory.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.modules.review.repository.ReviewRepository;
import backend.pineapple_ecommerce.infrastructure.cloudinary.CloudinaryService;
import backend.pineapple_ecommerce.common.util.FileValidator;
import backend.pineapple_ecommerce.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;
    private final CloudinaryService        cloudinaryService;
    private final FileValidator fileValidator;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setSlug(generateUniqueSlug(request.getName(), request.getSlug()));

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, slug={}", saved.getId(), saved.getSlug());
        return enrichDetailResponse(saved);
    }

    // ─────────────────────────────────────────────
    // UPLOAD IMAGES
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public List<UploadResponse> uploadProductImages(Long productId, List<MultipartFile> files) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (files == null || files.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một ảnh");
        }

        // Validate tất cả file
        for (MultipartFile file : files) {
            fileValidator.validateImage(file);
        }

        // Upload multiple
        List<UploadResponse> uploadResponses = cloudinaryService.uploadImages(files, UploadFolder.PRODUCT);

        // Lưu vào ProductImage
        for (UploadResponse resp : uploadResponses) {
            ProductImage image = ProductImage.builder()
                    .product(product)
                    .imageUrl(resp.getUrl())
                    .publicId(resp.getPublicId())
                    .build();
            product.getImages().add(image);
        }

        productRepository.save(product);
        return uploadResponses;
    }

    @Override
    @Transactional
    public UploadResponse uploadThumbnail(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // Validate file
        fileValidator.validateImage(file);

        // Upload thumbnail
        UploadResponse uploadResponse = cloudinaryService.uploadImage(file, UploadFolder.PRODUCT);

        // Xóa thumbnail cũ nếu có
        if (product.getThumbnail() != null && !product.getThumbnail().isBlank()) {
            cloudinaryService.deleteImage(product.getThumbnail());
        }

        product.setThumbnail(uploadResponse.getUrl());
        productRepository.save(product);

        return uploadResponse;
    }

    // ─────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return enrichDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findActiveBySlugWithImages(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product slug" + slug));
        return enrichDetailResponse(product);
    }

    // ─────────────────────────────────────────────
    // SEARCH — NEW 2.4: thêm farmId + inStock
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> searchProducts(
            String keyword,
            Long categoryId,
            Long farmId,          // NEW
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isOrganic,
            Boolean inStock,      // NEW
            int page, int size,
            String sortBy, String sortDir) {

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        boolean isBestSeller = "best_seller".equalsIgnoreCase(sortBy);

        // Nếu có farmId hoặc inStock → dùng query mở rộng (không hỗ trợ best_seller vì JOIN phức tạp)
        boolean needsExtended = (farmId != null) || (Boolean.TRUE.equals(inStock));

        Page<Product> productPage;

        if (needsExtended) {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = resolveSortField(isBestSeller ? "newest" : sortBy);
            productPage = productRepository.searchProductsExtended(
                    safeKeyword, categoryId, farmId, minPrice, maxPrice, isOrganic, inStock,
                    PageRequest.of(page, size, Sort.by(direction, sortField)));
        } else if (isBestSeller) {
            productPage = productRepository.searchProductsBestSeller(
                    safeKeyword, categoryId, minPrice, maxPrice, isOrganic,
                    PageRequest.of(page, size));
        } else {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            productPage = productRepository.searchProducts(
                    safeKeyword, categoryId, minPrice, maxPrice, isOrganic,
                    PageRequest.of(page, size, Sort.by(direction, resolveSortField(sortBy))));
        }

        return PageResponse.of(productPage.map(p -> enrichSummaryResponse(productMapper.toSummaryResponse(p), p)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getAllProductsForAdmin(int page, int size) {
        return getAllProductsForAdmin(page, size, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getAllProductsForAdmin(
            int page, int size, String keyword, String statusStr) {

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        ProductStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ProductStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        return PageResponse.of(productRepository
                .searchProductsForAdmin(safeKeyword, status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(p -> enrichSummaryResponse(productMapper.toSummaryResponse(p), p)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getRelatedProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getCategory() == null) return List.of();

        return productRepository.findRelatedProducts(
                        product.getCategory().getId(), productId,
                        PageRequest.of(0, limit, Sort.by("createdAt").descending()))
                .getContent().stream()
                .map(p -> enrichSummaryResponse(productMapper.toSummaryResponse(p), p))
                .toList();
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.getCategoryId() != null
                && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        }

        productMapper.updateFromRequest(request, product);

        if (request.getName() != null && !request.getName().equals(product.getName())) {
            product.setSlug(generateUniqueSlug(request.getName(), null));
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}", saved.getId());
        return enrichDetailResponse(saved);
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
        log.info("Product soft-deleted (INACTIVE): id={}", id);
    }

    // ─────────────────────────────────────────────
    // STOCK
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int getAvailableStock(Long productId) {
        Integer stock = inventoryBatchRepository.getTotalAvailableStock(productId);
        return stock != null ? stock : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getProductsByIds(List<Long> ids) {
        return productRepository.findAllById(ids).stream()
                .map(product -> enrichSummaryResponse(productMapper.toSummaryResponse(product), product))
                .toList();
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private ProductDetailResponse enrichDetailResponse(Product product) {
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        int stock = getAvailableStock(product.getId());
        response.setTotalStock(stock);
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());
        response.setAverageRating(avgRating != null ? avgRating : 0.0);
        int reviewCount = reviewRepository.countByProductId(product.getId());
        response.setReviewCount(reviewCount);
        return response;
    }

    private ProductSummaryResponse enrichSummaryResponse(ProductSummaryResponse summary, Product product) {
        summary.setTotalStock(getAvailableStock(product.getId()));
        return summary;
    }

    private String generateUniqueSlug(String name, String requestedSlug) {
        String base = (requestedSlug != null && !requestedSlug.isBlank())
                ? requestedSlug : SlugUtils.toSlug(name);
        String slug = base;
        int suffix = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String resolveSortField(String sortBy) {
        return switch (sortBy == null ? "newest" : sortBy.toLowerCase()) {
            case "price"   -> "price";
            case "name"    -> "name";
            case "rating"  -> "averageRating";
            default        -> "createdAt";
        };
    }

    private List<ProductImage> buildProductImages(List<UploadResponse> uploads, Product product) {
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < uploads.size(); i++) {
            UploadResponse u = uploads.get(i);
            images.add(ProductImage.builder()
                    .product(product)
                    .imageUrl(u.getUrl())
                    .publicId(u.getPublicId())
                    .sortOrder(i)
                    .build());
        }
        return images;
    }
}
