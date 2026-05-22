// ===== File: entity/Category.java =====
package backend.pineapple_ecommerce.modules.product.models;

import backend.pineapple_ecommerce.common.entity.BaseEntity;
import backend.pineapple_ecommerce.modules.category.models.Category;
import backend.pineapple_ecommerce.common.enums.ProductStatus;
import backend.pineapple_ecommerce.modules.review.models.Review;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.modules.wishlist.models.Wishlist;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_slug", columnList = "slug", unique = true),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_status", columnList = "status")
})
@NamedEntityGraph(
        name = "Product.withCategory",
        attributeNodes = @NamedAttributeNode("category")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;

    // Thông tin sản phẩm dứa
    @Column(precision = 8, scale = 2)
    private BigDecimal weight;          // Đơn vị: GRAM (ví dụ: 320.5 = 320.5 gram)

    @Column(precision = 6, scale = 2)
    private BigDecimal calories;        // kcal/100g

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String origin;              // Xuất xứ

    @Column(name = "is_organic", nullable = false)
    @Builder.Default
    private Boolean isOrganic = false;

    @Column(length = 500)
    private String thumbnail;

    @Column(name = "thumbnail_public_id", length = 300)
    private String thumbnailPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    // === Relationships ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<InventoryBatch> batches = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Wishlist> wishlistItems = new ArrayList<>();

    // Helper: tính giá hiển thị (ưu tiên discount)
    public BigDecimal getEffectivePrice() {
        return discountPrice != null ? discountPrice : price;
    }
}