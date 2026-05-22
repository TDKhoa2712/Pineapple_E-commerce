package backend.pineapple_ecommerce.common.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả trả về sau khi upload ảnh lên Cloudinary.
 */
@Getter
@Builder
public class UploadResponse {

    /** URL truy cập ảnh (https). */
    private String url;

    /**
     * Public ID trên Cloudinary.
     * Lưu lại nếu cần xoá ảnh sau này.
     * VD: "pineapple-ecommerce/products/abc123"
     */
    private String publicId;

    /** Định dạng file: jpg, png, webp, ... */
    private String format;

    /** Kích thước file (bytes). */
    private long   bytes;

    /** Chiều rộng ảnh (px). */
    private int    width;

    /** Chiều cao ảnh (px). */
    private int    height;
}