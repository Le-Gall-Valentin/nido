package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.nido.api.authentication.domain.port.out.SessionSettingsPort;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CookieServiceSettingsTest {

    private final AtomicInteger accessMinutes = new AtomicInteger(15);
    private final AtomicInteger refreshDays = new AtomicInteger(30);
    private final AtomicBoolean secure = new AtomicBoolean(true);

    private final CookieService cookies = new CookieService(
        new SessionSettingsPort() {
            @Override public int accessTokenMinutes() { return accessMinutes.get(); }
            @Override public boolean secureCookies() { return secure.get(); }
        },
        (RefreshTokenConfigPort) refreshDays::get);

    @Test
    void each_cookie_is_built_with_the_settings_of_the_moment() {
        ResponseCookie before = cookies.buildAccessCookie("t");
        accessMinutes.set(60);
        refreshDays.set(7);
        secure.set(false);

        assertThat(before.getMaxAge()).isEqualTo(Duration.ofMinutes(15));
        assertThat(before.isSecure()).isTrue();
        assertThat(cookies.buildAccessCookie("t").getMaxAge()).isEqualTo(Duration.ofMinutes(60));
        assertThat(cookies.buildRefreshCookie("t").getMaxAge()).isEqualTo(Duration.ofDays(7));
        assertThat(cookies.buildRefreshCookie("t").isSecure()).isFalse();
    }
}
