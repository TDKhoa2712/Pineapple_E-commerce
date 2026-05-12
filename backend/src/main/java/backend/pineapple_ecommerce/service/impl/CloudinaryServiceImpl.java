package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.response.UploadResponse;
import backend.pineapple_ecommerce.enums.UploadFolder;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.service.CloudinaryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    // ─────────────────────────────────────────────
    // Giới hạn upload
    // ─────────────────────────────────────────────

    private static final long   MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    // ─────────────────────────────────────────────
    // UPLOAD SINGLE
    // ─────────────────────────────────────────────

    @Override
    public UploadResponse uploadImage(MultipartFile file, UploadFolder folder) {
        validateFile(file);

        try {
            // public_id = folder/uuid (không có extension — Cloudinary tự thêm)
            String publicId = folder.getPath() + "/" + UUID.randomUUID();

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id",        publicId,
                            "overwrite",        false,
                            "resource_type",    "image",
                            // Tự động chuyển sang WebP để tối ưu dung lượng
                            "fetch_format",     "auto",
                            "quality",          "auto:good"
                    )
            );

            log.info("Uploaded image: publicId={}, bytes={}", publicId, result.get("bytes"));
            return mapResult(result);

        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage(), e);
            throw new BusinessException("Tải ảnh lên thất bại: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // UPLOAD MULTIPLE
    // ─────────────────────────────────────────────

    @Override
    public List<UploadResponse> uploadImages(List<MultipartFile> files, UploadFolder folder) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > 10) {
            throw new BusinessException("Tối đa 10 ảnh mỗi lần upload");
        }

        List<UploadResponse> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadImage(file, folder));
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // DELETE SINGLE
    // ─────────────────────────────────────────────

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) return;

        try {
            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image")
            );

            String resultStatus = (String) result.get("result");
            if (!"ok".equals(resultStatus) && !"not found".equals(resultStatus)) {
                log.warn("Cloudinary delete returned unexpected status '{}' for publicId={}",
                        resultStatus, publicId);
            } else {
                log.info("Deleted image: publicId={}, result={}", publicId, resultStatus);
            }

        } catch (IOException e) {
            // Không throw — xoá ảnh thất bại không nên block flow chính
            log.error("Cloudinary delete failed for publicId={}: {}", publicId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // DELETE MULTIPLE
    // ─────────────────────────────────────────────

    @Override
    public void deleteImages(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) return;

        for (String publicId : publicIds) {
            deleteImage(publicId);
        }
    }

    // ─────────────────────────────────────────────
    // BUILD THUMBNAIL URL (on-the-fly transform)
    // ─────────────────────────────────────────────

    @Override
    public String buildThumbnailUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(
                        new Transformation<>()
                                .width(width)
                                .height(height)
                                .crop("fill")          // cắt giữ tỷ lệ, lấp đầy khung
                                .gravity("auto")       // Cloudinary AI chọn vùng quan trọng
                                .fetchFormat("auto")   // WebP nếu browser hỗ trợ
                                .quality("auto:good")
                )
                .secure(true)
                .generate(publicId);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File không được để trống");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(
                    String.format("Kích thước file vượt quá giới hạn %dMB", MAX_FILE_SIZE_BYTES / 1024 / 1024));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(
                    "Định dạng file không hợp lệ. Chỉ chấp nhận: JPEG, PNG, WebP, GIF");
        }
    }

    @SuppressWarnings("unchecked")
    private UploadResponse mapResult(Map<?, ?> result) {
        return UploadResponse.builder()
                .url(      (String)  result.get("secure_url"))
                .publicId( (String)  result.get("public_id"))
                .format(   (String)  result.get("format"))
                .bytes(    toLong(   result.get("bytes")))
                .width(    toInt(    result.get("width")))
                .height(   toInt(    result.get("height")))
                .build();
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return 0;
    }
}