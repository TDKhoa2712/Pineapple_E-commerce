package backend.pineapple_ecommerce.modules.product.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /**
     * Public ID trên Cloudinary.
     * VD: "pineapple-ecommerce/products/550e8400-e29b-..."
     * Lưu lại để xoá ảnh khỏi Cloudinary khi sản phẩm bị xoá
     * hoặc khi ảnh bị thay thế, tránh ảnh rác tích tụ.
     */
    @Column(name = "public_id", length = 300)
    private String publicId;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}