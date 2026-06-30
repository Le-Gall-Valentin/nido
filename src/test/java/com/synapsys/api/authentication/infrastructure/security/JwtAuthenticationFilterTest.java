package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.shared.model.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtValidationService jwtValidationService;
    @Mock CookieService cookieService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtValidationService, cookieService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_noCookie_chainProceedsWithNoAuthentication() throws Exception {
        when(cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE))
            .thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_validToken_setsAuthenticationInContext() throws Exception {
        var claims = new UserClaims(UUID.randomUUID(), Role.USER, "user@test.com");
        when(cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE))
            .thenReturn(Optional.of("valid.jwt.token"));
        when(jwtValidationService.validateAndExtract("valid.jwt.token")).thenReturn(claims);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
            .isInstanceOf(CustomUserDetails.class);
    }

    @Test
    void doFilter_invalidToken_chainProceedsWithNoAuthentication() throws Exception {
        when(cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE))
            .thenReturn(Optional.of("bad.token"));
        when(jwtValidationService.validateAndExtract("bad.token"))
            .thenThrow(new IllegalArgumentException("invalid signature"));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_unexpectedRuntimeException_propagates() throws Exception {
        when(cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE))
            .thenReturn(Optional.of("any.token"));
        when(jwtValidationService.validateAndExtract("any.token"))
            .thenThrow(new NullPointerException("null claim"));

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
            .isInstanceOf(NullPointerException.class);
    }
}