package com.nido.api.instance.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SettingKeyTest {

    @Test
    void the_smtp_password_alone_is_a_secret_and_the_public_address_alone_is_required() {
        assertThat(Arrays.stream(SettingKey.values()).filter(SettingKey::secret)).containsExactly(SettingKey.MAIL_PASSWORD);
        assertThat(Arrays.stream(SettingKey.values()).filter(SettingKey::required)).containsExactly(SettingKey.PUBLIC_URL);
    }

    @Test
    void every_setting_is_found_by_its_code_and_belongs_to_its_group() {
        for (SettingKey key : SettingKey.values()) {
            assertThat(SettingKey.fromCode(key.code())).contains(key);
            assertThat(key.group().keys()).contains(key);
        }
        assertThat(SettingKey.fromCode("nope")).isEmpty();
    }
}
