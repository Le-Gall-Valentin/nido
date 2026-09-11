package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.port.out.IssuedTokenCutoffPort;
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
import java.time.Instant;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtValidationService jwtValidationService;
    private final CookieService cookieService;
    private final IssuedTokenCutoffPort issuedTokenCutoffPort;

    public JwtAuthenticationFilter(JwtValidationService jwtValidationService, CookieService cookieService,
                                   IssuedTokenCutoffPort issuedTokenCutoffPort) {
        this.jwtValidationService = jwtValidationService;
        this.cookieService = cookieService;
        this.issuedTokenCutoffPort = issuedTokenCutoffPort;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE).ifPresent(token -> {
            try {
                UserClaims claims = jwtValidationService.validateAndExtract(token);
                if (issuedBeforeTheUsersRightsChanged(claims)) {
                    // Leaving the context unauthenticated produces a 401, deliberately, and not a
                    // 403: the frontend refreshes on 401 and the refresh re-reads the user from the
                    // database, so the caller is handed a truthful token instead of being stuck.
                    log.debug("Token for user {} predates a cut-off — treating the request as anonymous",
                        claims.userId());
                    return;
                }
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

    /**
     * Strictly before, never "at or before": a token minted in the same second as the cut-off is
     * kept. {@code iat} has second precision, so rejecting equality would also reject the token the
     * refresh hands back within that second, and the caller would be bounced out instead of
     * recovering. The hole this leaves is a refresh landing in the very second of a demotion.
     */
    private boolean issuedBeforeTheUsersRightsChanged(UserClaims claims) {
        return issuedTokenCutoffPort.cutoffFor(claims.userId())
            .filter(cutoff -> claims.issuedAt().isBefore(truncateToSecond(cutoff)))
            .isPresent();
    }

    /** {@code iat} is stored to the second, so the cut-off has to be compared at that resolution. */
    private static Instant truncateToSecond(Instant moment) {
        return moment.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }
}