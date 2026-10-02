package com.nido.api.notifications.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationChannelTest {

    @Test
    void a_channel_is_found_by_its_code() {
        assertThat(NotificationChannel.EMAIL.code()).isEqualTo("email");
        assertThat(NotificationChannel.fromCode("email")).contains(NotificationChannel.EMAIL);
    }

    @Test
    void an_unknown_code_finds_nothing() {
        assertThat(NotificationChannel.fromCode("sms")).isEmpty();
        assertThat(NotificationChannel.fromCode("EMAIL")).isEmpty();
        assertThat(NotificationChannel.fromCode(null)).isEmpty();
    }
}
