package backend.pineapple_ecommerce.modules.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên không quá 100 ký tự")
    private String name;

    // slug sẽ tự generate từ name nếu để trống
    private String slug;

    private String image;

    // null = danh mục gốc
    private Long parentId;
}
