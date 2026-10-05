package com.nido.api.mail.domain.model;

/** Mail is on, and its links start with {@code appUrl} (no trailing slash). */
public record ActiveMail(String appUrl) {}
