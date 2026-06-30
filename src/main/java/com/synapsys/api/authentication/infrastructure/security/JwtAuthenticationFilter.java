package com.synapsys.api.authentication.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtValidationService jwtValidationService;
    private final CookieService cookieService;

    public JwtAuthenticationFilter(JwtValidationService jwtValidationService, CookieService cookieService) {
        this.jwtValidationService = jwtValidationService;
        this.cookieService = cookieService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE).ifPresent(token -> {
            try {
                UserClaims claims = jwtValidationService.validateAndExtract(token);
                var userDetails = new CustomUserDetails(claims.userId(), claims.role(), claims.email());
                var auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                log.debug("Invalid JWT token: {}", e.getMessage());
            }
        });

        chain.doFilter(request, response);
    }
}