package backend.pineapple_ecommerce.modules.address.models;

import backend.pineapple_ecommerce.common.entity.BaseEntity;
import backend.pineapple_ecommerce.modules.user.models.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Địa chỉ giao hàng của người dùng.
 *
 * <p>Thay đổi so với phiên bản cũ:
 * <ul>
 *   <li>Bỏ {@code ghnDistrictId}, {@code ghnWardCode} (hard-couple với GHN)
 *   <li>Thêm {@code carrierMetadata} (JSON) — lưu ID địa chỉ của từng carrier
 * </ul>
 *
 * <p>Cấu trúc {@code carrierMetadata}:
 * <pre>
 * {
 *   "GHN":         { "districtId": 1454, "wardCode": "21307" },
 *   "GHTK":        { "province": "Hồ Chí Minh", "district": "Quận 1" },
 *   "VIETTEL_POST":{ "provinceCode": "HCM", "districtCode": "Q1" }
 * }
 * </pre>
 *
 * <p>Khi thêm carrier mới, chỉ cần thêm key mới vào JSON — không cần migration DB.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String ward;

    @Column(nullable = false, length = 255)
    private String detail;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "carrier_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String carrierMetadata;
}