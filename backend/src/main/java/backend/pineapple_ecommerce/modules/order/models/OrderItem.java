package backend.pineapple_ecommerce.modules.order.models;

import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private InventoryBatch batch;

    @Column(name = "batch_code", length = 50)
    private String batchCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;   // Lưu giá tại thời điểm mua

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Snapshot tên sản phẩm (đề phòng bị xoá/đổi tên sau)
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_thumbnail", length = 500)
    private String productThumbnail;
}
