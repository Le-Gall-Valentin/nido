package com.nido.api.instance.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Every setting the administration page can change: its code, its group, the variable that overrides
 * it, and its default. Adding a setting is adding a line here — no migration: rows are keyed by code.
 * The defaults live here, not in application.yaml, or "set in the environment" could not be told
 * apart from "left at its default".
 */
public enum SettingKey {
    MAIL_HOST("mail.host", SettingGroup.MAIL, "NIDO_SMTP_HOST", null),
    MAIL_PORT("mail.port", SettingGroup.MAIL, "NIDO_SMTP_PORT", "587"),
    MAIL_SECURITY("mail.security", SettingGroup.MAIL, "NIDO_SMTP_SECURITY", "starttls"),
    MAIL_USERNAME("mail.username", SettingGroup.MAIL, "NIDO_SMTP_USERNAME", null),
    MAIL_PASSWORD("mail.password", SettingGroup.MAIL, "NIDO_SMTP_PASSWORD", null),
    MAIL_FROM("mail.from", SettingGroup.MAIL, "NIDO_MAIL_FROM", null),
    PUBLIC_URL("public-url", SettingGroup.PUBLIC_URL, "NIDO_APP_URL", null),
    ACCESS_TOKEN_MINUTES("sessions.access-token-minutes", SettingGroup.SESSIONS, "NIDO_JWT_EXPIRY_MINUTES", "15"),
    REFRESH_TOKEN_DAYS("sessions.refresh-token-days", SettingGroup.SESSIONS, "NIDO_REFRESH_TOKEN_EXPIRY_DAYS", "30"),
    SWAGGER("api.swagger", SettingGroup.API, "SWAGGER_ENABLED", "false");

    private final String code;
    private final SettingGroup group;
    private final String variable;
    private final String defaultValue;

    SettingKey(String code, SettingGroup group, String variable, String defaultValue) {
        this.code = code;
        this.group = group;
        this.variable = variable;
        this.defaultValue = defaultValue;
    }

    public String code() { return code; }
    public SettingGroup group() { return group; }
    public String variable() { return variable; }
    /** Null when the setting has no default: not set means off (mail) or unknown (the public address). */
    public String defaultValue() { return defaultValue; }

    /** Stored encrypted, never sent back to a browser, never logged. */
    public boolean secret() {
        return this == MAIL_PASSWORD;
    }

    public static Optional<SettingKey> fromCode(String code) {
        return Arrays.stream(values()).filter(key -> key.code.equals(code)).findFirst();
    }
}
