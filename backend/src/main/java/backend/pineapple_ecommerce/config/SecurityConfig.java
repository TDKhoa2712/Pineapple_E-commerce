package backend.pineapple_ecommerce.config;

import backend.pineapple_ecommerce.security.JwtAuthenticationFilter;
import backend.pineapple_ecommerce.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 configuration — Stateless JWT.
 *
 * Role hierarchy:
 *   ROLE_ADMIN   — toàn quyền
 *   ROLE_FARMER  — quản lý farm, inventory
 *   ROLE_USER    — mua hàng, cart, wishlist, order của chính mình
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // kích hoạt @PreAuthorize, @Secured
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // ─────────────────────────────────────────────
    // Public endpoints (không cần token)
    // ─────────────────────────────────────────────
    private static final String[] PUBLIC_GET = {
            "/api/v1/products/**",
            "/api/v1/categories/**",
            "/api/v1/reviews/**",
            "/api/v1/payments/vnpay-return",
    };

    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
    };

    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
    };

    // ─────────────────────────────────────────────
    // Security Filter Chain
    // ─────────────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)           // REST API — không dùng CSRF
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Swagger & docs
                    .requestMatchers(SWAGGER_WHITELIST).permitAll()

                    // Auth endpoints
                    .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()

                    // Public reads
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()

                    // Admin only
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                    // Farmer + Admin
                    .requestMatchers(HttpMethod.GET, "/api/v1/farms/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/farms/**").hasAnyRole("FARMER", "ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/farms/**").hasAnyRole("FARMER", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/farms/**").hasAnyRole("FARMER", "ADMIN")
                    .requestMatchers("/api/v1/inventory/**").hasAnyRole("FARMER", "ADMIN")

                    // Authenticated users
                    .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─────────────────────────────────────────────
    // Beans
    // ─────────────────────────────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
