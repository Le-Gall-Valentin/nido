package com.nido.api.mail;

import com.nido.api.mail.domain.model.MailContent;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Facts written by the kit, with the values a template formats itself — a test fixture, never sent. */
public record FactsSampleMail(BigDecimal amount, LocalDate date) implements MailContent {

    @Override
    public String template() {
        return "test/facts-sample";
    }
}
