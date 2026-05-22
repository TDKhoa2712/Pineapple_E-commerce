package backend.pineapple_ecommerce.infrastructure.cloudinary;

import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import backend.pineapple_ecommerce.common.enums.UploadFolder;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.util.FileValidator;
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
    private final FileValidator fileValidator;

    // ─────────────────────────────────────────────
    // UPLOAD SINGLE
    // ─────────────────────────────────────────────

    @Override
    public UploadResponse uploadImage(MultipartFile file, UploadFolder folder) {
        fileValidator.validateImage(file);

        try {
            String publicId = folder.getPath() + "/" + UUID.randomUUID();

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id",     publicId,
                            "overwrite",     false,
                            "resource_type", "image",
                            "fetch_format",  "auto",
                            "quality",       "auto:good"
                    )
            );

            log.info("Uploaded image: publicId={}, size={} bytes", publicId, result.get("bytes"));
            return mapResult(result);

        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new BusinessException("Tải ảnh lên thất bại");
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
    // DELETE
    // ─────────────────────────────────────────────

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) return;

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
            log.info("Deleted image: {}", publicId);
        } catch (Exception e) {
            log.warn("Failed to delete image {}: {}", publicId, e.getMessage());
        }
    }

    @Override
    public void deleteImages(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) return;
        publicIds.forEach(this::deleteImage);
    }

    // ─────────────────────────────────────────────
    // THUMBNAIL URL
    // ─────────────────────────────────────────────

    @Override
    public String buildThumbnailUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new Transformation<>()
                        .width(width)
                        .height(height)
                        .crop("fill")
                        .gravity("auto")
                        .fetchFormat("auto")
                        .quality("auto:good"))
                .secure(true)
                .generate(publicId);
    }

    // ─────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private UploadResponse mapResult(Map<?, ?> result) {
        return UploadResponse.builder()
                .url((String) result.get("secure_url"))
                .publicId((String) result.get("public_id"))
                .format((String) result.get("format"))
                .bytes(toLong(result.get("bytes")))
                .width(toInt(result.get("width")))
                .height(toInt(result.get("height")))
                .build();
    }

    private long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private int toInt(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }
}