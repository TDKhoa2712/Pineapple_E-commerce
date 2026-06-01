package backend.pineapple_ecommerce.modules.product.controller;

import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import backend.pineapple_ecommerce.common.enums.UploadFolder;
import backend.pineapple_ecommerce.common.util.FileValidator;
import backend.pineapple_ecommerce.infrastructure.cloudinary.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "Upload", description = "Quản lý upload ảnh lên Cloudinary")
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final CloudinaryService cloudinaryService;
    private final FileValidator fileValidator;

    @Operation(summary = "Upload ảnh lên Cloudinary (General)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "PRODUCT") String folderStr) {

        log.info("Request to upload image, folder={}", folderStr);
        UploadFolder folder = UploadFolder.PRODUCT;
        try {
            folder = UploadFolder.valueOf(folderStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid folder type: {}, fallback to PRODUCT", folderStr);
        }

        fileValidator.validateImage(file);
        UploadResponse response = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.ok(ApiResponse.success(response, "Upload ảnh thành công"));
    }
}
