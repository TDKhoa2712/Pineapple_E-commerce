package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class UserResponse {

    private Long   id;
    private String email;
    private String fullName;
    private String phone;

    /** URL ảnh đại diện. */
    private String avatar;

    /**
     * Public ID Cloudinary của avatar.
     * Frontend thường không cần field này, nhưng trả về
     * để admin hoặc các service nội bộ có thể tham chiếu.
     */
    private String avatarPublicId;

    private String            status;
    private Set<String>       roles;
    private LocalDateTime     createdAt;
    private LocalDateTime     updatedAt;
}