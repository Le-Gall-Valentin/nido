package com.nido.api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** A compose line `NIDO_COOKIE_SECURE: ${NIDO_COOKIE_SECURE}` with nothing in .env hands the app an empty value. */
class NidoPropertiesBindingTest {

    @EnableConfigurationProperties(NidoProperties.class)
    static class Config {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void an_empty_cookie_secure_says_nothing() {
        runner.withPropertyValues("nido.cookie.secure=").run(context -> {
            NidoProperties.CookieProperties cookie = context.getBean(NidoProperties.class).cookie();
            assertThat(cookie == null ? null : cookie.secure()).isNull();
        });
    }

    @Test
    void false_says_false() {
        runner.withPropertyValues("nido.cookie.secure=false").run(context ->
            assertThat(context.getBean(NidoProperties.class).cookie().secure()).isFalse());
    }

    @Test
    void nothing_is_required_any_more() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }
}
