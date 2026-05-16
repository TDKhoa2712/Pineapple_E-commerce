package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FarmResponse {
    private Long   id;
    private Long   ownerId;
    private String ownerName;
    private String name;
    private String location;
    private String description;
    private String certificate;
    private String imageUrl;

    /** NEW: trạng thái farm (PENDING_APPROVAL / ACTIVE / INACTIVE / REJECTED) */
    private String status;

    /**
     * NEW: lý do từ chối — chỉ có giá trị khi status = REJECTED.
     * Farmer xem detail farm của mình để biết lý do.
     */
    private String rejectionReason;

    private LocalDateTime createdAt;
}
