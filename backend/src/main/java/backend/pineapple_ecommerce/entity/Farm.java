package backend.pineapple_ecommerce.entity;

import backend.pineapple_ecommerce.enums.FarmStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "farms", indexes = {
    @Index(name = "idx_farms_owner", columnList = "owner_id"),
    @Index(name = "idx_farms_status", columnList = "status")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Farm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String certificate;

    @Column(length = 500)
    private String imageUrl;

    /** PUBLIC_ID để xoá ảnh cũ trên Cloudinary khi thay ảnh mới */
    @Column(name = "image_public_id", length = 200)
    private String imagePublicId;

    // ─────────────────────────────────────────────
    // NEW FIELDS — 2.5 FarmService
    // ─────────────────────────────────────────────

    /**
     * Trạng thái farm trong marketplace.
     * Farm mới tạo mặc định PENDING_APPROVAL — chưa hiển thị public.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private FarmStatus status = FarmStatus.PENDING_APPROVAL;

    /**
     * Soft delete — xoá farm không xoá dữ liệu thật,
     * chỉ ẩn khỏi mọi listing và không cho phép thêm batch.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Lý do từ chối — chỉ có giá trị khi status = REJECTED.
     * Farmer xem trong detail farm của mình.
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // ─────────────────────────────────────────────
    // Relationships
    // ─────────────────────────────────────────────

    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL)
    @Builder.Default
    private List<InventoryBatch> batches = new ArrayList<>();
}
