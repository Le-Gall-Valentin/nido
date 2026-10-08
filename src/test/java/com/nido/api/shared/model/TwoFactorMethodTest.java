package com.nido.api.shared.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorMethodTest {

    @Test
    void a_url_names_a_method_in_lower_case_only() {
        assertThat(TwoFactorMethod.APP.pathSegment()).isEqualTo("app");
        assertThat(TwoFactorMethod.fromPathSegment("mail")).contains(TwoFactorMethod.MAIL);
        assertThat(TwoFactorMethod.fromPathSegment("MAIL")).isEmpty();
        assertThat(TwoFactorMethod.fromPathSegment("sms")).isEmpty();
    }

    @Test
    void responses_list_the_app_before_the_mail_whatever_order_they_came_in() {
        assertThat(TwoFactorMethod.ordered(Set.of(TwoFactorMethod.MAIL, TwoFactorMethod.APP)))
            .containsExactly(TwoFactorMethod.APP, TwoFactorMethod.MAIL);
        assertThat(TwoFactorMethod.ordered(List.of())).isEmpty();
    }
}
