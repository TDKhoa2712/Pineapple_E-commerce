package backend.pineapple_ecommerce.security;

import backend.pineapple_ecommerce.common.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility — tạo, parse và validate Access Token.
 * Dùng JJWT 0.12.x (spring-boot-starter-security kéo sẵn).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    // ────────────────────────────────────────────
    // Generate
    // ────────────────────────────────────────────

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(Map.of(), userDetails, jwtProperties.getAccessTokenExpirationMs());
    }

    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtProperties.getAccessTokenExpirationMs());
    }

    private String buildToken(Map<String, Object> extraClaims,
                              UserDetails userDetails,
                              long expirationMs) {
        Map<String, Object> claims = new java.util.HashMap<>(extraClaims);

        if (userDetails instanceof CustomUserDetails customUser) {
            claims.put("userId", customUser.getUserId());
            claims.put("fullName", customUser.getFullName());
            claims.put("avatar", customUser.getAvatar());
        }

        if (userDetails.getAuthorities() != null) {
            java.util.List<String> rolesList = userDetails.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .collect(java.util.stream.Collectors.toList());
            claims.put("roles", rolesList);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())   // username = email
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ────────────────────────────────────────────
    // Extract
    // ────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public CustomUserDetails buildUserDetailsFromClaims(Claims claims) {
        String email = claims.getSubject();
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            return null;
        }
        Long userId = null;
        if (userIdObj instanceof Number) {
            userId = ((Number) userIdObj).longValue();
        }
        String fullName = claims.get("fullName", String.class);
        String avatar = claims.get("avatar", String.class);
        
        java.util.List<?> rolesList = claims.get("roles", java.util.List.class);
        if (rolesList == null || rolesList.isEmpty()) {
            return null;
        }
        
        java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = rolesList.stream()
                .map(Object::toString)
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());
                
        return CustomUserDetails.of(userId, email, fullName, avatar, authorities);
    }

    // ────────────────────────────────────────────
    // Validate
    // ────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // ────────────────────────────────────────────
    // Key
    // ────────────────────────────────────────────

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpirationMs();
    }
}
