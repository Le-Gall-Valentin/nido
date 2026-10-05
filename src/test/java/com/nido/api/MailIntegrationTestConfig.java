package com.nido.api;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link IntegrationTestConfig} with mail switched on, delivering to {@link SharedGreenMail}. Every
 * class using it shares one Spring context: keep the properties below identical for all of them.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTestConfig
@ExtendWith(SharedGreenMail.Starter.class)
@TestPropertySource(properties = {
    "NIDO_SMTP_HOST=127.0.0.1",
    "NIDO_SMTP_PORT=" + SharedGreenMail.PORT,
    "NIDO_SMTP_SECURITY=none",
    "NIDO_MAIL_FROM=Nido <nido@test.local>",
    "NIDO_APP_URL=http://localhost:5173"
})
public @interface MailIntegrationTestConfig {
}
