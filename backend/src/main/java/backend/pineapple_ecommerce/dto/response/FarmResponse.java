package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FarmResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String name;
    private String location;
    private String description;
    private String certificate;
    private String imageUrl;
    private LocalDateTime createdAt;
}
