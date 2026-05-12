package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.CreateProductRequest;
import backend.pineapple_ecommerce.dto.request.UpdateProductRequest;
import backend.pineapple_ecommerce.dto.response.*;
import backend.pineapple_ecommerce.entity.*;
import backend.pineapple_ecommerce.enums.ProductStatus;
import backend.pineapple_ecommerce.enums.UploadFolder;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.mapper.ProductMapper;
import backend.pineapple_ecommerce.repository.*;
import backend.pineapple_ecommerce.service.CloudinaryService;
import backend.pineapple_ecommerce.service.ProductService;
import backend.pineapple_ecommerce.util.SlugUtils;
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

    private final ProductRepository        productRepository;
    private final CategoryRepository       categoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final ReviewRepository         reviewRepository;
    private final ProductMapper            productMapper;
    private final CloudinaryService        cloudinaryService;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        // 1. Validate & lấy Category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        // 2. Map request → entity
        Product product = productMapper.toEntity(request);
        product.setCategory(category);

        // 3. Sinh slug unique
        product.setSlug(generateUniqueSlug(request.getName(), request.getSlug()));

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, slug={}", saved.getId(), saved.getSlug());

        return enrichDetailResponse(saved);
    }

    // ─────────────────────────────────────────────
    // UPLOAD PRODUCT IMAGES
    // ─────────────────────────────────────────────

    /**
     * Upload (hoặc thay thế) toàn bộ ảnh phụ của sản phẩm.
     *
     * <p>Flow:
     * <ol>
     *   <li>Upload các file mới lên Cloudinary → nhận về danh sách UploadResponse.</li>
     *   <li>Thu thập publicId của ảnh cũ.</li>
     *   <li>Xoá danh sách {@code ProductImage} cũ khỏi DB (orphanRemoval sẽ xử lý).</li>
     *   <li>Thêm danh sách {@code ProductImage} mới (đã có publicId) vào product.</li>
     *   <li>Save product → xoá ảnh cũ khỏi Cloudinary (batch delete).</li>
     * </ol>
     *
     * Upload trước, xoá sau: nếu upload thất bại thì ảnh cũ vẫn nguyên vẹn.
     */
    @Override
    @Transactional
    public List<UploadResponse> uploadProductImages(Long productId, List<MultipartFile> images) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        // 1. Upload ảnh mới lên Cloudinary
        List<UploadResponse> uploaded = cloudinaryService.uploadImages(images, UploadFolder.PRODUCT);

        // 2. Ghi nhớ publicId cũ để xoá sau
        List<String> oldPublicIds = product.getImages().stream()
                .map(ProductImage::getPublicId)
                .filter(pid -> pid != null && !pid.isBlank())
                .toList();

        // 3. Xoá ảnh cũ khỏi DB
        product.getImages().clear();

        // 4. Thêm ảnh mới (có publicId)
        List<ProductImage> newImages = buildProductImages(uploaded, product);
        product.getImages().addAll(newImages);

        productRepository.save(product);
        log.info("Uploaded {} images for productId={}", uploaded.size(), productId);

        // 5. Xoá ảnh cũ khỏi Cloudinary (sau khi DB đã commit thành công)
        //    deleteImages() nuốt exception nội bộ — không ảnh hưởng response
        if (!oldPublicIds.isEmpty()) {
            cloudinaryService.deleteImages(oldPublicIds);
            log.info("Deleted {} old Cloudinary images for productId={}", oldPublicIds.size(), productId);
        }

        // 6. Trả về danh sách UploadResponse khớp với Controller
        return uploaded;
    }
    @Override
    @Transactional
    public UploadResponse uploadThumbnail(Long productId, MultipartFile file) {
        // 1. Tìm sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        // 2. Upload ảnh mới lên Cloudinary (folder PRODUCT)
        UploadResponse uploaded = cloudinaryService.uploadImage(file, UploadFolder.PRODUCT);

        // 3. Ghi nhớ publicId cũ (nếu có) để chuẩn bị xoá
        // Lưu ý: Nhớ thêm field thumbnailPublicId vào entity Product nhé!
        String oldPublicId = product.getThumbnailPublicId();

        // 4. Cập nhật URL và PublicId mới vào Entity
        product.setThumbnail(uploaded.getUrl());
        product.setThumbnailPublicId(uploaded.getPublicId());

        // 5. Lưu xuống Database
        productRepository.save(product);
        log.info("Thumbnail updated for productId={}, new publicId={}", productId, uploaded.getPublicId());

        // 6. Xoá ảnh cũ khỏi Cloudinary (Chỉ thực hiện sau khi save DB thành công)
        if (oldPublicId != null && !oldPublicId.isBlank()) {
            cloudinaryService.deleteImage(oldPublicId);
            log.info("Deleted old thumbnail publicId={} for productId={}", oldPublicId, productId);
        }

        return uploaded;
    }
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
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return enrichDetailResponse(product);
    }

    // ─────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> searchProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isOrganic,
            int page, int size,
            String sortBy, String sortDir) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = List.of("price", "name", "createdAt").contains(sortBy)
                ? sortBy : "createdAt";
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<Product> productPage = productRepository.searchByKeyword(keyword, pageable);

        Page<ProductSummaryResponse> result = productPage.map(p -> {
            ProductSummaryResponse summary = productMapper.toSummaryResponse(p);
            return enrichSummaryResponse(summary, p);
        });

        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getAllProductsForAdmin(int page, int size) {
        Page<ProductSummaryResponse> result = productRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(p -> {
                    ProductSummaryResponse summary = productMapper.toSummaryResponse(p);
                    return enrichSummaryResponse(summary, p);
                });
        return PageResponse.of(result);
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getStatus() != null) {
            try {
                product.setStatus(ProductStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Status không hợp lệ: " + request.getStatus());
            }
        }

        // Partial update từ request (null-safe qua MapStruct)
        productMapper.updateFromRequest(request, product);

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}", saved.getId());
        return enrichDetailResponse(saved);
    }

    // ─────────────────────────────────────────────
    // DELETE (soft delete + Cloudinary cleanup)
    // ─────────────────────────────────────────────

    /**
     * Xoá mềm sản phẩm (status → INACTIVE).
     * Đồng thời xoá toàn bộ ảnh phụ khỏi Cloudinary để tránh tích tụ ảnh rác.
     */
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        // Thu thập publicId của tất cả ảnh phụ
        List<String> publicIds = product.getImages().stream()
                .map(ProductImage::getPublicId)
                .filter(pid -> pid != null && !pid.isBlank())
                .toList();

        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
        log.info("Product soft-deleted: id={}", id);

        // Xoá ảnh khỏi Cloudinary sau khi DB đã update
        if (!publicIds.isEmpty()) {
            cloudinaryService.deleteImages(publicIds);
            log.info("Deleted {} Cloudinary images for productId={}", publicIds.size(), id);
        }
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

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private String generateUniqueSlug(String name, String providedSlug) {
        String base = (providedSlug != null && !providedSlug.isBlank())
                ? SlugUtils.toSlug(providedSlug)
                : SlugUtils.toSlug(name);

        if (!productRepository.existsBySlug(base)) {
            return base;
        }

        int suffix = 1;
        String candidate;
        do {
            candidate = base + "-" + suffix++;
        } while (productRepository.existsBySlug(candidate));

        return candidate;
    }

    /**
     * Map kết quả Cloudinary upload → List<ProductImage> với sortOrder tăng dần.
     * Mỗi ProductImage lưu cả URL lẫn publicId để dễ xoá sau này.
     */
    private List<ProductImage> buildProductImages(List<UploadResponse> uploads, Product product) {
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < uploads.size(); i++) {
            UploadResponse up = uploads.get(i);
            images.add(ProductImage.builder()
                    .product(product)
                    .imageUrl(up.getUrl())
                    .publicId(up.getPublicId())   // ← lưu publicId để xoá sau
                    .sortOrder(i)
                    .build());
        }
        return images;
    }

    private ProductDetailResponse enrichDetailResponse(Product product) {
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        int stock = getAvailableStock(product.getId());
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());
        int reviewCount = (int) reviewRepository.findByProductId(
                product.getId(), PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();

        return ProductDetailResponse.builder()
                .id(response.getId())
                .name(response.getName())
                .slug(response.getSlug())
                .description(response.getDescription())
                .price(response.getPrice())
                .discountPrice(response.getDiscountPrice())
                .effectivePrice(response.getEffectivePrice())
                .weight(response.getWeight())
                .calories(response.getCalories())
                .brand(response.getBrand())
                .origin(response.getOrigin())
                .isOrganic(response.getIsOrganic())
                .thumbnail(response.getThumbnail())
                .status(response.getStatus())
                .categoryId(response.getCategoryId())
                .categoryName(response.getCategoryName())
                .imageUrls(response.getImageUrls())
                .totalStock(stock)
                .averageRating(avgRating)
                .reviewCount(reviewCount)
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    private ProductSummaryResponse enrichSummaryResponse(ProductSummaryResponse summary, Product product) {
        int stock = getAvailableStock(product.getId());
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());

        return ProductSummaryResponse.builder()
                .id(summary.getId())
                .name(summary.getName())
                .slug(summary.getSlug())
                .price(summary.getPrice())
                .discountPrice(summary.getDiscountPrice())
                .effectivePrice(summary.getEffectivePrice())
                .thumbnail(summary.getThumbnail())
                .isOrganic(summary.getIsOrganic())
                .status(summary.getStatus())
                .categoryName(summary.getCategoryName())
                .totalStock(stock)
                .averageRating(avgRating)
                .build();
    }
}