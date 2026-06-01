package backend.pineapple_ecommerce.common.config;

import backend.pineapple_ecommerce.modules.auth.service.CustomOAuth2UserService;
import backend.pineapple_ecommerce.security.JwtAuthenticationFilter;
import backend.pineapple_ecommerce.security.OAuth2AuthenticationFailureHandler;
import backend.pineapple_ecommerce.security.OAuth2AuthenticationSuccessHandler;
import backend.pineapple_ecommerce.modules.user.service.UserDetailsServiceImpl;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import backend.pineapple_ecommerce.security.CsrfCookieFilter;


import java.util.List;

/**
 * SecurityConfig — cập nhật để hỗ trợ OAuth2 Social Login.
 *
 * Thay đổi so với phiên bản cũ:
 * 1. Inject CustomOAuth2UserService, SuccessHandler, FailureHandler
 * 2. Thêm .oauth2Login() block vào SecurityFilterChain
 * 3. Thêm OAuth2 callback endpoint vào PUBLIC_GET whitelist
 * 4. Session vẫn STATELESS — Spring Security tạo session tạm thời cho OAuth2 flow
 *    (bắt buộc bởi OAuth2 spec để lưu state/nonce), sau đó SuccessHandler
 *    redirect về FE kèm JWT và session không được dùng nữa.
 *
 * Lưu ý quan trọng: OAuth2 flow BẮT BUỘC cần session tạm thời (state param).
 * SessionCreationPolicy.STATELESS sẽ block OAuth2 nếu không có cấu hình đặc biệt.
 * Giải pháp: dùng IF_REQUIRED thay vì STATELESS, hoặc cấu hình HttpSessionOAuth2AuthorizationRequestRepository.
 * File này dùng IF_REQUIRED — session chỉ tạo khi cần (OAuth2 flow), JWT filter
 * vẫn là primary auth mechanism cho mọi API call khác.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter            jwtAuthFilter;
    private final UserDetailsServiceImpl             userDetailsService;
    private final CorsProperties                     corsProperties;
    private final CustomOAuth2UserService            customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    // ─────────────────────────────────────────────
    // Public endpoints
    // ─────────────────────────────────────────────

    private static final String[] PUBLIC_GET = {
            "/api/v1/products/**",
            "/api/v1/categories/**",
            "/api/v1/reviews/**",
            "/api/v1/payments/vnpay-return",
            "/api/v1/payments/vnpay-ipn",
            "/api/v1/shipping/provinces",
           "/api/v1/shipping/districts",
           "/api/v1/shipping/wards",
    };

    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/oauth2/exchange",
            "/api/v1/auth/password-reset/**",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/shipping/calculate-fee",
            "/api/v1/webhooks/shipping/**",
    };

    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
    };

    private static final String[] OAUTH2_WHITELIST = {
            "/oauth2/authorization/**",
            "/login/oauth2/code/**",
    };

    // ─────────────────────────────────────────────
    // OAuth2 Authorization Request Repository
    // Khai báo explicit để dễ swap sang Cookie/Redis implementation
    // ─────────────────────────────────────────────

    @Bean
    public HttpSessionOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setSecure(cookieSecure);
        repository.setCookiePath("/");
        return repository;
    }

    // ─────────────────────────────────────────────
    // Security Filter Chain
    // ─────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                // Webhooks & Payment notifications
                                "/api/v1/webhooks/**",
                                "/api/v1/webhooks/ghn",
                                "/api/v1/payments/vnpay-ipn",
                                // Public Auth endpoints
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/password-reset/**",
                                // Swagger/Docs
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        )
                        .csrfTokenRepository(csrfTokenRepository())
                )

                // IF_REQUIRED: session chỉ tạo khi cần (OAuth2 state param)
                // Mọi API call vẫn stateless qua JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .requestMatchers(OAUTH2_WHITELIST).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers("/api/v1/webhooks/ghn").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/farms", "/api/v1/farms/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/farms", "/api/v1/farms/**").hasAnyRole("USER", "FARMER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/farms", "/api/v1/farms/**").hasAnyRole("USER", "FARMER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/farms", "/api/v1/farms/**").hasAnyRole("USER", "FARMER", "ADMIN")
                        .requestMatchers("/api/v1/inventory/**").hasAnyRole("FARMER", "ADMIN")
                        .requestMatchers("/api/v1/upload/**").hasAnyRole("USER", "FARMER", "ADMIN")
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        // Explicit khai báo repo — nhất quán state giữa request và callback
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestRepository(authorizationRequestRepository())
                        )
                        .redirectionEndpoint(redirect -> redirect
                                .baseUri("/login/oauth2/code/*")
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

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
        config.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

