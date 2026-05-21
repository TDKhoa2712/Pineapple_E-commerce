// src/main/java/backend/pineapple_ecommerce/util/FileValidator.java
package backend.pineapple_ecommerce.util;

import backend.pineapple_ecommerce.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Component
public class FileValidator {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Kích thước file không được vượt quá 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Chỉ chấp nhận file ảnh (JPEG, PNG, WebP, GIF)");
        }

        // Kiểm tra magic bytes
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || !isValidImageMagicBytes(bytes)) {
                throw new BusinessException("File không phải là ảnh hợp lệ");
            }
        } catch (IOException e) {
            throw new BusinessException("Không thể đọc file upload");
        }
    }

    private boolean isValidImageMagicBytes(byte[] bytes) {
        if (bytes.length < 4) return false;

        // JPEG
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) return true;
        // PNG
        if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) return true;
        // GIF
        if (bytes[0] == (byte) 'G' && bytes[1] == (byte) 'I' && bytes[2] == (byte) 'F') return true;
        // WebP
        if (bytes[0] == (byte) 'R' && bytes[1] == (byte) 'I' && bytes[2] == (byte) 'F' && bytes[3] == (byte) 'F') return true;

        return false;
    }
}