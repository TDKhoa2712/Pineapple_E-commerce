package backend.pineapple_ecommerce.modules.product.service;

import backend.pineapple_ecommerce.modules.product.dto.request.CreateProductRequest;
import backend.pineapple_ecommerce.modules.product.dto.request.UpdateProductRequest;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.modules.product.dto.response.ProductDetailResponse;
import backend.pineapple_ecommerce.modules.product.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductDetailResponse createProduct(CreateProductRequest request);

    List<UploadResponse> uploadProductImages(Long productId, List<MultipartFile> images);

    UploadResponse uploadThumbnail(Long productId, MultipartFile file);

    ProductDetailResponse getProductById(Long id);

    ProductDetailResponse getProductBySlug(String slug);

    /**
     * Tìm kiếm + lọc sản phẩm ACTIVE (public).
     * Tất cả filter đều optional (null = bỏ qua).
     * sortBy: price_asc | price_desc | newest | best_seller | name
     *
     * NEW — 2.4: thêm farmId và inStock.
     * farmId  : lọc sản phẩm thuộc một farm cụ thể.
     * inStock : true = chỉ hiển thị sản phẩm còn hàng.
     */
    PageResponse<ProductSummaryResponse> searchProducts(
            String keyword,
            Long categoryId,
            Long farmId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isOrganic,
            Boolean inStock,
            int page, int size,
            String sortBy, String sortDir);

    /** Compat: gọi không có farmId và inStock — dùng cho code cũ */
    default PageResponse<ProductSummaryResponse> searchProducts(
            String keyword, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice, Boolean isOrganic,
            int page, int size, String sortBy, String sortDir) {
        return searchProducts(keyword, categoryId, null, minPrice, maxPrice, isOrganic,
                null, page, size, sortBy, sortDir);
    }

    /** Admin: lấy tất cả sản phẩm (không lọc status). */
    PageResponse<ProductSummaryResponse> getAllProductsForAdmin(int page, int size);

    /** Admin: tìm kiếm sản phẩm với keyword và lọc theo status. */
    PageResponse<ProductSummaryResponse> getAllProductsForAdmin(
            int page, int size, String keyword, String statusStr, String sortBy, String sortDirection);

    /** Lấy sản phẩm liên quan (cùng danh mục, loại trừ sản phẩm hiện tại). */
    List<ProductSummaryResponse> getRelatedProducts(Long productId, int limit);

    ProductDetailResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id, Long requesterId);

    PageResponse<ProductSummaryResponse> getMyProducts(Long ownerId, int page, int size, String keyword, String status, String sortBy, String sortDirection);

    int getAvailableStock(Long productId);

    List<ProductSummaryResponse> getProductsByIds(List<Long> ids);
}
