package backend.pineapple_ecommerce.controller;

import backend.pineapple_ecommerce.dto.request.CreateProductRequest;
import backend.pineapple_ecommerce.dto.request.UpdateProductRequest;
import backend.pineapple_ecommerce.dto.response.*;
import backend.pineapple_ecommerce.service.ProductService;
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

    // ─────────────────────────────────────────────
    // PUBLIC — GET (no auth required)
    // ─────────────────────────────────────────────

    @Operation(summary = "Tìm kiếm & lọc sản phẩm (public)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isOrganic,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")       String sortDir) {

        PageResponse<ProductSummaryResponse> result = productService.searchProducts(
                keyword, categoryId, minPrice, maxPrice, isOrganic, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Lấy chi tiết sản phẩm theo ID (public)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @Operation(summary = "Lấy chi tiết sản phẩm theo slug (public)")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    // ─────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────

    @Operation(summary = "Lấy tất cả sản phẩm phân trang (Admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getAllForAdmin(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(productService.getAllProductsForAdmin(page, size)));
    }

    @Operation(summary = "Tạo sản phẩm mới (Admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {

        ProductDetailResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Tạo sản phẩm thành công"));
    }

    @Operation(summary = "Cập nhật sản phẩm (Admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {

        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, request), "Cập nhật thành công"));
    }

    @Operation(summary = "Xoá mềm sản phẩm (Admin)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xoá sản phẩm thành công"));
    }

    @Operation(summary = "Lấy tồn kho sản phẩm")
    @GetMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Integer>> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getAvailableStock(id)));
    }

    // ProductController.java — thêm 2 endpoint upload

    @Operation(summary = "Upload thumbnail cho sản phẩm (Admin)")
    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadThumbnail(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.uploadThumbnail(id, file), "Upload thumbnail thành công"));
    }

    @Operation(summary = "Upload nhiều ảnh gallery cho sản phẩm (Admin)")
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UploadResponse>>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.uploadProductImages(id, files), "Upload ảnh thành công"));
    }
}
