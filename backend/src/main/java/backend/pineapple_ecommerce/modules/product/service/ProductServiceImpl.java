package backend.pineapple_ecommerce.modules.product.service;

import backend.pineapple_ecommerce.modules.product.dto.response.ProductDetailResponse;
import backend.pineapple_ecommerce.modules.product.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import backend.pineapple_ecommerce.modules.product.mapper.ProductMapper;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
import backend.pineapple_ecommerce.common.enums.ProductStatus;
import backend.pineapple_ecommerce.common.enums.FarmStatus;
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
import backend.pineapple_ecommerce.modules.product.specification.ProductSpecification;
import backend.pineapple_ecommerce.modules.farm.repository.FarmRepository;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.enums.RoleName;
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
    private final CloudinaryService cloudinaryService;
    private final FileValidator fileValidator;
    private final CacheManager cacheManager;
    private final FarmRepository farmRepository;
    private final UserService userService;

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

        // Extract public ID for thumbnail if it's Cloudinary
        if (product.getThumbnail() != null) {
            product.setThumbnailPublicId(extractPublicId(product.getThumbnail()));
        }

        // Map request.getImageUrls() to product.getImages()
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String url = request.getImageUrls().get(i);
                images.add(ProductImage.builder()
                        .product(product)
                        .imageUrl(url)
                        .publicId(extractPublicId(url))
                        .sortOrder(i)
                        .build());
            }
            product.setImages(images);
        }

        User current = userService.getEntityUser(userService.getCurrentUserId());
        product.setCreatedBy(current);
        boolean isAdmin = current.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            ensureUserHasActiveFarm(current.getId());
            product.setStatus(ProductStatus.INACTIVE);
        }

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

        User current = userService.getEntityUser(userService.getCurrentUserId());
        boolean isAdmin = current.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            if (product.getCreatedBy() == null || !product.getCreatedBy().getId().equals(current.getId())) {
                throw new UnauthorizedException("Bạn không có quyền chỉnh sửa sản phẩm này");
            }
            ensureProductFarmIsActive(product, current.getId());
            product.setStatus(ProductStatus.INACTIVE);
        }

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

        User current = userService.getEntityUser(userService.getCurrentUserId());
        boolean isAdmin = current.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            if (product.getCreatedBy() == null || !product.getCreatedBy().getId().equals(current.getId())) {
                throw new UnauthorizedException("Bạn không có quyền chỉnh sửa sản phẩm này");
            }
            ensureProductFarmIsActive(product, current.getId());
            product.setStatus(ProductStatus.INACTIVE);
        }

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
    @Cacheable(value = "products", key = "#id")
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return enrichDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#slug")
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
            Long farmId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isOrganic,
            Boolean inStock,
            int page, int size,
            String sortBy, String sortDir) {

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        boolean sortByRelevance = (safeKeyword != null) &&
                (sortBy == null || sortBy.isBlank() || "relevance".equalsIgnoreCase(sortBy));

        Specification<Product> spec = Specification.allOf(
                ProductSpecification.fetchCategory(),
                ProductSpecification.hasStatus(ProductStatus.ACTIVE),
                ProductSpecification.hasCategory(categoryId),
                ProductSpecification.hasFarmId(farmId),
                ProductSpecification.hasPriceGreaterThanOrEqual(minPrice),
                ProductSpecification.hasPriceLessThanOrEqual(maxPrice),
                ProductSpecification.isOrganic(isOrganic),
                ProductSpecification.inStock(inStock),
                ProductSpecification.searchByKeyword(safeKeyword, sortByRelevance)
        );

        if ("best_seller".equalsIgnoreCase(sortBy)) {
            spec = spec.and(ProductSpecification.sortByBestSeller());
        }

        Pageable pageable;
        if (sortByRelevance || "best_seller".equalsIgnoreCase(sortBy)) {
            pageable = PageRequest.of(page, size, Sort.unsorted());
        } else {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            pageable = PageRequest.of(page, size, Sort.by(direction, resolveSortField(sortBy)));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductSummaryResponse> enrichedList = enrichSummaryResponses(productPage.getContent());
        Page<ProductSummaryResponse> summaryPage = new PageImpl<>(enrichedList, productPage.getPageable(), productPage.getTotalElements());

        return PageResponse.of(summaryPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getAllProductsForAdmin(int page, int size) {
        return getAllProductsForAdmin(page, size, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getAllProductsForAdmin(
            int page, int size, String keyword, String statusStr, String sortBy, String sortDirection) {

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        ProductStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ProductStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Specification<Product> spec = Specification.allOf(
                ProductSpecification.fetchCategory(),
                ProductSpecification.hasStatus(status),
                ProductSpecification.searchByKeyword(safeKeyword, safeKeyword != null)
        );

        Pageable pageable;
        if (safeKeyword != null && (sortBy == null || sortBy.isBlank())) {
            pageable = PageRequest.of(page, size, Sort.unsorted());
        } else {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String resolvedSortBy = resolveSortField(sortBy);
            pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
        }
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductSummaryResponse> enrichedList = enrichSummaryResponses(productPage.getContent());
        Page<ProductSummaryResponse> summaryPage = new PageImpl<>(enrichedList, productPage.getPageable(), productPage.getTotalElements());

        return PageResponse.of(summaryPage);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products_related", key = "#productId + '_' + #limit")
    public List<ProductSummaryResponse> getRelatedProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getCategory() == null) return List.of();

        List<Product> relatedProducts = productRepository.findRelatedProducts(
                        product.getCategory().getId(), productId,
                        PageRequest.of(0, limit, Sort.by("createdAt").descending()))
                .getContent();
        return enrichSummaryResponses(relatedProducts);
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        User current = userService.getEntityUser(userService.getCurrentUserId());
        boolean isAdmin = current.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            if (product.getCreatedBy() == null || !product.getCreatedBy().getId().equals(current.getId())) {
                throw new UnauthorizedException("Bạn không có quyền chỉnh sửa sản phẩm này");
            }
            ensureProductFarmIsActive(product, current.getId());
            product.setStatus(ProductStatus.INACTIVE);
        }

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

        // Extract public ID for thumbnail if updated
        if (request.getThumbnail() != null) {
            product.setThumbnailPublicId(extractPublicId(request.getThumbnail()));
        }

        // Map request.getImageUrls() to product.getImages()
        if (request.getImageUrls() != null) {
            product.getImages().clear();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String url = request.getImageUrls().get(i);
                product.getImages().add(ProductImage.builder()
                        .product(product)
                        .imageUrl(url)
                        .publicId(extractPublicId(url))
                        .sortOrder(i)
                        .build());
            }
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}", saved.getId());
        ProductDetailResponse response = enrichDetailResponse(saved);
        evictProductCache(saved.getId(), saved.getSlug());
        return response;
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteProduct(Long id, Long requesterId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        User current = userService.getEntityUser(requesterId);
        boolean isAdmin = current.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            if (product.getCreatedBy() == null || !product.getCreatedBy().getId().equals(current.getId())) {
                throw new UnauthorizedException("Bạn không có quyền xoá sản phẩm này");
            }
            ensureProductFarmIsActive(product, current.getId());
        }

        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
        log.info("Product soft-deleted (INACTIVE): id={} by user={}", id, requesterId);
        evictProductCache(product.getId(), product.getSlug());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getMyProducts(
            Long ownerId, int page, int size, String keyword, String statusStr, String sortBy, String sortDirection) {

        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        ProductStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ProductStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Specification<Product> spec = Specification.allOf(
                ProductSpecification.fetchCategory(),
                ProductSpecification.createdByUser(ownerId),
                ProductSpecification.hasStatus(status),
                ProductSpecification.searchByKeyword(safeKeyword, safeKeyword != null)
        );

        Pageable pageable;
        if (safeKeyword != null && (sortBy == null || sortBy.isBlank())) {
            pageable = PageRequest.of(page, size, Sort.unsorted());
        } else {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String resolvedSortBy = resolveSortField(sortBy);
            pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
        }
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductSummaryResponse> enrichedList = enrichSummaryResponses(productPage.getContent());
        Page<ProductSummaryResponse> summaryPage = new PageImpl<>(enrichedList, productPage.getPageable(), productPage.getTotalElements());

        return PageResponse.of(summaryPage);
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
    @Cacheable(value = "products_by_ids", key = "#ids")
    public List<ProductSummaryResponse> getProductsByIds(List<Long> ids) {
        List<Product> products = productRepository.findAllById(ids);
        return enrichSummaryResponses(products);
    }

    private void evictProductCache(Long id, String slug) {
        try {
            Cache productsCache = cacheManager.getCache("products");
            if (productsCache != null) {
                productsCache.evict(id);
                if (slug != null) {
                    productsCache.evict(slug);
                }
            }
            Cache relatedCache = cacheManager.getCache("products_related");
            if (relatedCache != null) {
                relatedCache.clear();
            }
            Cache byIdsCache = cacheManager.getCache("products_by_ids");
            if (byIdsCache != null) {
                byIdsCache.clear();
            }
        } catch (Exception e) {
            log.error("Failed to evict product cache", e);
        }
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private void ensureUserHasActiveFarm(Long ownerId) {
        if (!farmRepository.existsByOwnerIdAndStatusAndIsDeletedFalse(ownerId, FarmStatus.ACTIVE)) {
            throw new BusinessException("Trang trai cua ban chua hoat dong nen khong the thao tac san pham.");
        }
    }

    private void ensureProductFarmIsActive(Product product, Long ownerId) {
        if (inventoryBatchRepository.existsProductInNonActiveFarm(product.getId(), ownerId)) {
            throw new BusinessException("Trang trai cua san pham dang ngung hoat dong nen khong the thao tac san pham.");
        }
        ensureUserHasActiveFarm(ownerId);
    }

    private ProductDetailResponse enrichDetailResponse(Product product) {
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        int stock = getAvailableStock(product.getId());
        response.setTotalStock(stock);
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());
        response.setAverageRating(avgRating != null ? avgRating : 0.0);
        int reviewCount = reviewRepository.countByProductId(product.getId());
        response.setReviewCount(reviewCount);

        if (product.getCreatedBy() != null) {
            farmRepository.findByOwnerIdAndIsDeletedFalse(product.getCreatedBy().getId())
                    .stream().findFirst().ifPresent(farm -> {
                        response.setFarmId(farm.getId());
                        response.setFarmName(farm.getName());
                    });
        }
        return response;
    }

    private List<ProductSummaryResponse> enrichSummaryResponses(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        List<Long> ids = products.stream().map(Product::getId).toList();
        List<Object[]> stockResults = inventoryBatchRepository.getTotalAvailableStockByProductIds(ids);
        java.util.Map<Long, Integer> stockMap = stockResults.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        return products.stream()
                .map(p -> {
                    ProductSummaryResponse summary = productMapper.toSummaryResponse(p);
                    summary.setTotalStock(stockMap.getOrDefault(p.getId(), 0));

                    if (p.getCreatedBy() != null) {
                        farmRepository.findByOwnerIdAndIsDeletedFalse(p.getCreatedBy().getId())
                                .stream().findFirst().ifPresent(farm -> {
                                    summary.setFarmId(farm.getId());
                                    summary.setFarmName(farm.getName());
                                });
                    }
                    return summary;
                })
                .toList();
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

    private String extractPublicId(String url) {
        if (url == null || !url.startsWith("https://res.cloudinary.com/")) {
            return null;
        }
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;
            String path = url.substring(uploadIndex + 8); // after "/upload/"
            
            // Skip version if present (e.g. "v123456789/")
            if (path.startsWith("v")) {
                int firstSlash = path.indexOf('/');
                if (firstSlash != -1) {
                    path = path.substring(firstSlash + 1);
                }
            }
            
            // Remove file extension (e.g. ".jpg")
            int lastDot = path.lastIndexOf('.');
            if (lastDot != -1) {
                path = path.substring(0, lastDot);
            }
            return path;
        } catch (Exception e) {
            log.warn("Failed to extract publicId from Cloudinary URL: {}", url, e);
            return null;
        }
    }
}
