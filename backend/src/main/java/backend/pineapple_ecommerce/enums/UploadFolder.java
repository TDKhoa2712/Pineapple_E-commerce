package backend.pineapple_ecommerce.enums;

/**
 * Tên thư mục trên Cloudinary cho từng loại tài nguyên.
 * Giúp quản lý, tìm kiếm và xoá ảnh theo nhóm dễ dàng hơn.
 *
 * Cấu trúc trên Cloudinary:
 *   pineapple-ecommerce/
 *     ├── products/
 *     ├── categories/
 *     ├── farms/
 *     ├── reviews/
 *     └── avatars/
 */
public enum UploadFolder {

    PRODUCT("pineapple-ecommerce/products"),
    CATEGORY("pineapple-ecommerce/categories"),
    FARM("pineapple-ecommerce/farms"),
    REVIEW("pineapple-ecommerce/reviews"),
    AVATAR("pineapple-ecommerce/avatars");

    private final String path;

    UploadFolder(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}