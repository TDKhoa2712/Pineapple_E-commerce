package backend.pineapple_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;       // "Bearer"
    private Long   expiresIn;       // seconds
    private Long   userId;
    private String email;
    private String fullName;
    private Set<String> roles;
}
