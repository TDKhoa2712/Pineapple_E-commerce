package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String image;
    private Long parentId;
    private String parentName;

    // Dùng khi trả cây danh mục (nested)
    private List<CategoryResponse> children;
}
