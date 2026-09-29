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
    "nido.mail.host=127.0.0.1",
    "nido.mail.port=" + SharedGreenMail.PORT,
    "nido.mail.security=none",
    "nido.mail.from=Nido <nido@test.local>",
    "nido.mail.app-url=http://localhost:5173"
})
public @interface MailIntegrationTestConfig {
}
