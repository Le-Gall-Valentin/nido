package com.nido.api.mail.infrastructure.render;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Amounts, days and months as the app writes them, in the language of the mail — a template's {@code format}.
 * A record carries them raw: whose language the mail is written in is only known when it is delivered.
 *
 * <p>One locale per language, as {@code resolveLocale} picks for the app: France for French, the United Kingdom
 * for English. Euros always, as {@code formatAmount}: the currency is a fact of Nido, not of the reader's language.
 */
public final class MailFormat {

    private static final Currency EURO = Currency.getInstance("EUR");

    private final Locale locale;

    public MailFormat(Locale language) {
        this.locale = "fr".equals(language.getLanguage()) ? Locale.FRANCE : Locale.UK;
    }

    public String amount(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(EURO);
        return format.format(amount);
    }

    public String date(LocalDate date) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date);
    }

    public String month(YearMonth month) {
        return DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month);
    }

    /** What a value became: the value alone when it did not change, else {@code before → after}. */
    public String change(String before, String after) {
        return Objects.equals(before, after) ? after : before + " → " + after;
    }
}
