package backend.pineapple_ecommerce.modules.product.controller;

import backend.pineapple_ecommerce.modules.product.dto.response.ProductDetailResponse;
import backend.pineapple_ecommerce.modules.product.dto.response.ProductSummaryResponse;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.common.dto.response.PageResponse;
import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import backend.pineapple_ecommerce.modules.product.service.ProductService;
import backend.pineapple_ecommerce.modules.product.dto.request.CreateProductRequest;
import backend.pineapple_ecommerce.modules.product.dto.request.UpdateProductRequest;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Products", description = "Quản lý sản phẩm")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    // ─────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────

    @Operation(summary = "Tìm kiếm & lọc sản phẩm (public)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long farmId,        // NEW — 2.4
            @RequestParam(required = false) Boolean inStock,    // NEW — 2.4
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isOrganic,
            @RequestParam(defaultValue = "0")       int page,
            @RequestParam(defaultValue = "20")      int size,
            @RequestParam(defaultValue = "newest")  String sortBy,
            @RequestParam(defaultValue = "desc")    String sortDir) {

        // Chuẩn hoá sortBy có chiều đi kèm
        String resolvedSortBy  = sortBy;
        String resolvedSortDir = sortDir;
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            resolvedSortBy = "price"; resolvedSortDir = "asc";
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            resolvedSortBy = "price"; resolvedSortDir = "desc";
        }

        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(
                keyword, categoryId, farmId, minPrice, maxPrice, isOrganic, inStock,
                page, size, resolvedSortBy, resolvedSortDir)));
    }

    @Operation(summary = "Lấy chi tiết sản phẩm theo ID (public)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    /**
     * NEW — 2.4: SEO-friendly URL. FE dùng slug thay vì ID trên product detail page.
     * VD: GET /api/v1/products/slug/dua-viet-nam-hoa-loc
     */
    @Operation(summary = "Lấy chi tiết sản phẩm theo Slug (SEO-friendly)")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    /**
     * Sản phẩm liên quan — cùng danh mục, giới hạn số lượng.
     * Đã có trong ProductService, thêm endpoint để FE gọi được.
     */
    @Operation(summary = "Sản phẩm liên quan (cùng category)")
    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getRelated(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRelatedProducts(id, limit)));
    }

    @Operation(summary = "Tổng tồn kho khả dụng của sản phẩm (public)")
    @GetMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Integer>> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getAvailableStock(id)));
    }

    // ─────────────────────────────────────────────
    // ADMIN / FARMER
    // ─────────────────────────────────────────────

    @Operation(summary = "Tạo sản phẩm mới (Admin/Farmer)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.createProduct(request), "Tạo sản phẩm thành công"));
    }

    @Operation(summary = "Upload ảnh sản phẩm (Admin/Farmer)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UploadResponse>>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.uploadProductImages(id, files), "Upload ảnh thành công"));
    }

    @Operation(summary = "Upload thumbnail sản phẩm (Admin/Farmer)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadThumbnail(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.uploadThumbnail(id, file), "Upload thumbnail thành công"));
    }

    @Operation(summary = "Lấy tất cả sản phẩm (Admin — filter keyword + status + sort)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getAllAdmin(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllProductsForAdmin(page, size, keyword, status, sortBy, sortDirection)));
    }

    @Operation(summary = "Lấy tất cả sản phẩm của tôi (Farmer/Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getMyProducts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        Long ownerId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                productService.getMyProducts(ownerId, page, size, keyword, status, sortBy, sortDirection)));
    }

    @Operation(summary = "Cập nhật sản phẩm (Admin/Farmer)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.updateProduct(id, request), "Cập nhật sản phẩm thành công"));
    }

    @Operation(summary = "Xoá sản phẩm — soft delete, chuyển INACTIVE (Admin/Farmer)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long requesterId = userService.getCurrentUserId();
        productService.deleteProduct(id, requesterId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá sản phẩm"));
    }
}
