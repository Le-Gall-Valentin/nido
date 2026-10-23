package com.nido.api.mail.infrastructure.render;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** What a mail says of an amount, a day or a month: what the app shows, in the mail's own language. */
class MailFormatTest {

    private final MailFormat french = new MailFormat(Locale.FRENCH);
    private final MailFormat english = new MailFormat(Locale.ENGLISH);

    @Test
    void an_amount_is_in_euros_as_the_app_writes_it() {
        assertThat(french.amount(new BigDecimal("1234.56"))).isEqualTo("1 234,56 €");
        assertThat(english.amount(new BigDecimal("1234.56"))).isEqualTo("€1,234.56");
        assertThat(french.amount(new BigDecimal("86.4"))).isEqualTo("86,40 €");
    }

    @Test
    void a_day_is_written_out() {
        assertThat(french.date(LocalDate.of(2026, 10, 21))).isEqualTo("21 octobre 2026");
        assertThat(english.date(LocalDate.of(2026, 10, 21))).isEqualTo("21 October 2026");
    }

    @Test
    void a_month_is_written_out() {
        assertThat(french.month(YearMonth.of(2026, 10))).isEqualTo("octobre 2026");
        assertThat(english.month(YearMonth.of(2026, 10))).isEqualTo("October 2026");
    }

    @Test
    void a_change_shows_before_and_after_only_when_they_differ() {
        assertThat(french.change("80,00 €", "80,00 €")).isEqualTo("80,00 €");
        assertThat(french.change("80,00 €", "86,40 €")).isEqualTo("80,00 € → 86,40 €");
    }

    @Test
    void a_language_the_app_does_not_speak_is_written_as_in_english() {
        assertThat(new MailFormat(Locale.GERMAN).amount(new BigDecimal("1234.56"))).isEqualTo("€1,234.56");
    }
}
