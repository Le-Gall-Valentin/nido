package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CookieServiceTest {

    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        var properties = new SynapsysProperties(
            new SynapsysProperties.JwtProperties("secret", 15, "synapsys-api", "synapsys-api"),
            new SynapsysProperties.RefreshTokenProperties(30, "0 0 3 * * *"),
            new SynapsysProperties.CookieProperties(false),
            null,
            new SynapsysProperties.CorsProperties(List.of()),
            new SynapsysProperties.RateLimitProperties(List.of()),
            new SynapsysProperties.EncryptionProperties("test-enc-secret"),
            null
        );
        cookieService = new CookieService(properties);
    }

    @Test
    void buildAccessCookie_isHttpOnlyOnApiPath() {
        ResponseCookie cookie = cookieService.buildAccessCookie("token123");

        assertThat(cookie.getName()).isEqualTo(CookieService.ACCESS_COOKIE);
        assertThat(cookie.getValue()).isEqualTo("token123");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(15 * 60);
    }

    @Test
    void buildRefreshCookie_hasRestrictedPath() {
        ResponseCookie cookie = cookieService.buildRefreshCookie("refresh123");

        assertThat(cookie.getName()).isEqualTo(CookieService.REFRESH_COOKIE);
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(30 * 86_400);
    }

    @Test
    void buildAccessCookie_hasSameSiteStrict() {
        ResponseCookie cookie = cookieService.buildAccessCookie("token123");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void buildClearCookies_setsMaxAgeZero() {
        List<ResponseCookie> cookies = cookieService.buildClearCookies();

        assertThat(cookies).hasSize(2);
        assertThat(cookies).allMatch(c -> c.getMaxAge().getSeconds() == 0);
        assertThat(cookies).allMatch(c -> c.getValue().isEmpty());
    }

    @Test
    void extractFromRequest_findsCookieByName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "x"), new Cookie(CookieService.ACCESS_COOKIE, "mytoken"));

        Optional<String> result = cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE);

        assertThat(result).contains("mytoken");
    }

    @Test
    void extractFromRequest_returnsEmptyWhenNoCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        Optional<String> result = cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE);

        assertThat(result).isEmpty();
    }

    @Test
    void extractFromRequest_returnsEmptyWhenCookieNotPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "value"));

        Optional<String> result = cookieService.extractFromRequest(request, CookieService.ACCESS_COOKIE);

        assertThat(result).isEmpty();
    }
}