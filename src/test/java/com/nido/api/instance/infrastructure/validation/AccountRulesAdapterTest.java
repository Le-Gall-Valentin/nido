package com.nido.api.instance.infrastructure.validation;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRulesAdapterTest {

    private final AccountRulesAdapter rules = new AccountRulesAdapter(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void an_address_and_a_password_the_setup_screen_accepts_hold() {
        assertThat(rules.emailProblem("admin@example.fr")).isEmpty();
        assertThat(rules.passwordProblem("Str0ng!Password")).isEmpty();
    }

    @Test
    void what_the_setup_screen_refuses_is_refused_without_being_quoted() {
        assertThat(rules.emailProblem("not-an-address")).hasValueSatisfying(problem -> assertThat(problem).doesNotContain("not-an-address"));
        for (String weak : new String[]{"changeme", "Short1!", "alllowercase1!", "NoDigits!!", "NoSpecial123", "É".repeat(40) + "1!A"}) {
            assertThat(rules.passwordProblem(weak)).as(weak).hasValueSatisfying(problem -> assertThat(problem).doesNotContain(weak));
        }
    }
}
