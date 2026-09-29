package com.nido.api.mail.domain.model;

/** A mail once written: its subject and both bodies. */
public record RenderedMail(String subject, String html, String text) {
}
