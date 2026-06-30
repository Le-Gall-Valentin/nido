package com.synapsys.api.infrastructure.config;

import com.synapsys.api.authentication.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtAuthenticationFilter jwtFilter,
                                    SynapsysProperties properties,
                                    UnauthorizedEntryPoint unauthorizedEntryPoint,
                                    ForbiddenAccessDeniedHandler forbiddenAccessDeniedHandler) throws Exception {
        return http
            // CSRF disabled: all state-changing requests require a valid HttpOnly cookie (SameSite=Strict).
            // Cross-origin requests are blocked by CORS. No CSRF token header needed.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                // TOTP challenge verification — authenticated via challenge cookie, not JWT
                .requestMatchers(HttpMethod.POST, "/api/auth/2fa/verify").permitAll()
                // /api/** must be evaluated before the SPA fallback so extensionless
                // paths like /api/users/me are never matched by the GET wildcard.
                .requestMatchers("/api/**").authenticated()
                // SPA fallback: extensionless GET paths are served as index.html
                .requestMatchers(HttpMethod.GET, "/{path:[^.]*}", "/**/{path:[^.]*}").permitAll()
                .anyRequest().denyAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedEntryPoint)
                .accessDeniedHandler(forbiddenAccessDeniedHandler)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "style-src 'self' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "object-src 'none'; " +
                    "img-src 'self' data:; " +
                    "base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .build();
    }

    private CorsConfigurationSource corsConfigurationSource(SynapsysProperties properties) {
        List<String> allowedOrigins = properties.cors() != null
            ? properties.cors().allowedOrigins().stream().filter(s -> !s.isBlank()).toList()
            : List.of();
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins.isEmpty()) {
            // Production: SPA served from same origin — no cross-origin requests needed
            config.setAllowedOrigins(List.of());
        } else {
            config.setAllowedOrigins(allowedOrigins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-Requested-With"));
            config.setExposedHeaders(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "Retry-After"));
            config.setAllowCredentials(true);
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
