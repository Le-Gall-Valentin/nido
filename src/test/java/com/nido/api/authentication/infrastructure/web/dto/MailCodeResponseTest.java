package com.nido.api.authentication.infrastructure.web.dto;

import com.nido.api.authentication.domain.model.MailCodeDelivery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailCodeResponseTest {

    @Test
    void a_code_sent_says_when_another_can_be_asked_for() {
        assertThat(MailCodeResponse.of(new MailCodeDelivery.Sent(60))).isEqualTo(new MailCodeResponse(true, 60L, null));
    }

    @Test
    void a_code_held_back_says_when_one_can_leave() {
        assertThat(MailCodeResponse.of(new MailCodeDelivery.TooSoon(40))).isEqualTo(new MailCodeResponse(false, null, 40L));
        assertThat(MailCodeResponse.of(new MailCodeDelivery.LimitReached(420))).isEqualTo(new MailCodeResponse(false, null, 420L));
    }

    @Test
    void a_code_that_cannot_leave_has_no_code_screen_to_describe() {
        // Mail went off before the code could leave: the sign-in lets the password through and never gets here.
        assertThatThrownBy(() -> MailCodeResponse.of(new MailCodeDelivery.Unavailable()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
