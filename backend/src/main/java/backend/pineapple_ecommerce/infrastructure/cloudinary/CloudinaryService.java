package backend.pineapple_ecommerce.infrastructure.cloudinary;

import backend.pineapple_ecommerce.common.dto.response.UploadResponse;
import backend.pineapple_ecommerce.common.enums.UploadFolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service tích hợp Cloudinary — upload, xoá, và transform ảnh.
 */
public interface CloudinaryService {

    /**
     * Upload một file ảnh lên Cloudinary.
     *
     * @param file   file MultipartFile từ request
     * @param folder thư mục đích trên Cloudinary (theo UploadFolder enum)
     * @return metadata ảnh sau khi upload
     */
    UploadResponse uploadImage(MultipartFile file, UploadFolder folder);

    /**
     * Upload nhiều ảnh cùng lúc.
     *
     * @param files  danh sách file
     * @param folder thư mục đích
     * @return danh sách metadata tương ứng
     */
    List<UploadResponse> uploadImages(List<MultipartFile> files, UploadFolder folder);

    /**
     * Xoá ảnh khỏi Cloudinary theo public ID.
     * Gọi khi xoá sản phẩm, thay ảnh, hoặc xoá đánh giá.
     *
     * @param publicId public ID trả về từ {@link #uploadImage}
     */
    void deleteImage(String publicId);

    /**
     * Xoá nhiều ảnh cùng lúc (batch delete).
     *
     * @param publicIds danh sách public ID cần xoá
     */
    void deleteImages(List<String> publicIds);

    /**
     * Tạo URL ảnh với transformation (resize, crop, chất lượng, ...).
     * Dùng khi cần hiển thị thumbnail kích thước cố định mà không cần upload lại.
     *
     * @param publicId public ID của ảnh gốc
     * @param width    chiều rộng mong muốn (px)
     * @param height   chiều cao mong muốn (px)
     * @return URL đã được transform
     */
    String buildThumbnailUrl(String publicId, int width, int height);
}