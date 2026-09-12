package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.port.out.IssuedTokenCutoffPort;
import com.nido.api.shared.model.Role;
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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtValidationService jwtValidationService;
    @Mock CookieService cookieService;
    @Mock IssuedTokenCutoffPort issuedTokenCutoffPort;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtValidationService, cookieService, issuedTokenCutoffPort);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * A token whose signature is perfectly valid but whose bearer no longer has the rights it
     * claims. Nothing in the token itself can say so — the cut-off recorded when those rights
     * changed is the only thing that can.
     */
    @Test
    void doFilter_tokenIssuedBeforeTheCutoff_isTreatedAsAnonymous() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.parse("2026-09-11T10:00:00Z");
        var claims = new UserClaims(userId, Role.ADMIN, "demoted@test.com", issuedAt);
        when(cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE)).thenReturn(Optional.of("t"));
        when(jwtValidationService.validateAndExtract("t")).thenReturn(claims);
        when(issuedTokenCutoffPort.cutoffFor(userId)).thenReturn(Optional.of(issuedAt.plusSeconds(30)));

        filter.doFilterInternal(request, response, chain);

        // Anonymous, not rejected outright: the chain still runs, so the security config decides
        // the status — a 401 on a protected route, which is what makes the frontend refresh.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_tokenIssuedAfterTheCutoff_stillAuthenticates() throws Exception {
        // The token handed back by the refresh that followed the demotion. Rejecting this one too
        // would bounce the user out of the application instead of letting them carry on demoted.
        UUID userId = UUID.randomUUID();
        Instant cutoff = Instant.parse("2026-09-11T10:00:00Z");
        var claims = new UserClaims(userId, Role.USER, "demoted@test.com", cutoff.plusSeconds(5));
        when(cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE)).thenReturn(Optional.of("t"));
        when(jwtValidationService.validateAndExtract("t")).thenReturn(claims);
        when(issuedTokenCutoffPort.cutoffFor(userId)).thenReturn(Optional.of(cutoff));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilter_tokenIssuedInTheSameSecondAsTheCutoff_isKept() throws Exception {
        // iat has second precision. Rejecting equality would also reject the token the refresh
        // returns within that second, and the caller would be logged out instead of recovering.
        UUID userId = UUID.randomUUID();
        Instant second = Instant.parse("2026-09-11T10:00:00Z");
        var claims = new UserClaims(userId, Role.USER, "user@test.com", second);
        when(cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE)).thenReturn(Optional.of("t"));
        when(jwtValidationService.validateAndExtract("t")).thenReturn(claims);
        when(issuedTokenCutoffPort.cutoffFor(userId)).thenReturn(Optional.of(second.plusMillis(400)));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilter_noCutoffRecorded_authenticatesNormally() throws Exception {
        // The overwhelmingly common case, and the one that must cost nothing: no key, no rejection.
        UUID userId = UUID.randomUUID();
        var claims = new UserClaims(userId, Role.USER, "user@test.com", Instant.now());
        when(cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE)).thenReturn(Optional.of("t"));
        when(jwtValidationService.validateAndExtract("t")).thenReturn(claims);
        when(issuedTokenCutoffPort.cutoffFor(userId)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
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
        var claims = new UserClaims(UUID.randomUUID(), Role.USER, "user@test.com", Instant.now());
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