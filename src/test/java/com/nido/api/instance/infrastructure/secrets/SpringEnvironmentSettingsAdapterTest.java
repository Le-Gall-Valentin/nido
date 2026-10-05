package com.nido.api.instance.infrastructure.secrets;

import com.nido.api.instance.domain.model.SettingKey;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SpringEnvironmentSettingsAdapterTest {

    @Test
    void only_the_variables_that_say_something_count() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("NIDO_SMTP_HOST", "")
            .withProperty("NIDO_COOKIE_SECURE", "")
            .withProperty("NIDO_SMTP_PORT", "   ")
            .withProperty("NIDO_JWT_EXPIRY_MINUTES", "20");

        assertThat(new SpringEnvironmentSettingsAdapter(environment).values())
            .containsExactly(java.util.Map.entry(SettingKey.ACCESS_TOKEN_MINUTES, "20"));
    }
}
