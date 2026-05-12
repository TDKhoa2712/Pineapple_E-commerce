package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.request.CreateProductRequest;
import backend.pineapple_ecommerce.dto.request.UpdateProductRequest;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.ProductDetailResponse;
import backend.pineapple_ecommerce.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.dto.response.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    /** Tạo sản phẩm mới. Tự động sinh slug từ tên; xử lý trùng slug bằng suffix. */
    ProductDetailResponse createProduct(CreateProductRequest request);

    /**
     * Upload ảnh phụ cho sản phẩm (thay thế toàn bộ danh sách ảnh cũ).
     * <p>
     * Mỗi ảnh được upload lên Cloudinary (folder PRODUCT), publicId lưu vào
     * {@code ProductImage.publicId}. Ảnh cũ bị xoá khỏi Cloudinary trước khi
     * thay bằng danh sách ảnh mới — không để lại ảnh rác.
     *
     * @param productId ID sản phẩm cần cập nhật ảnh
     * @param images    danh sách file ảnh (tối đa 10, mỗi file ≤ 10 MB)
     * @return thông tin sản phẩm sau khi cập nhật
     */
    List<UploadResponse> uploadProductImages(Long productId, List<MultipartFile> images);

    UploadResponse uploadThumbnail(Long productId, MultipartFile file);

    /** Lấy chi tiết sản phẩm theo id (bao gồm tồn kho, rating). */
    ProductDetailResponse getProductById(Long id);

    /** Lấy chi tiết sản phẩm theo slug — dùng cho frontend public. */
    ProductDetailResponse getProductBySlug(String slug);

    /**
     * Tìm kiếm + lọc sản phẩm ACTIVE phân trang.
     * keyword: tìm theo tên; categoryId: lọc theo danh mục;
     * minPrice / maxPrice: lọc theo giá; isOrganic: lọc hữu cơ.
     */
    PageResponse<ProductSummaryResponse> searchProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isOrganic,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    /** Lấy tất cả sản phẩm phân trang — dành cho Admin (không lọc status). */
    PageResponse<ProductSummaryResponse> getAllProductsForAdmin(int page, int size);

    /** Cập nhật thông tin sản phẩm (partial update — chỉ field không null). */
    ProductDetailResponse updateProduct(Long id, UpdateProductRequest request);

    /** Xoá mềm: chuyển status → INACTIVE, đồng thời xoá toàn bộ ảnh khỏi Cloudinary. */
    void deleteProduct(Long id);

    /** Lấy tồn kho hiện tại của sản phẩm (tổng remainingQuantity các batch AVAILABLE). */
    int getAvailableStock(Long productId);
}