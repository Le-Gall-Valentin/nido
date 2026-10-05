package com.nido.api.mail.domain.model;

/**
 * What is wrong with one field of a mail configuration. Fields: host, port, security, username,
 * password, from, appUrl. The codes are those the settings pages word.
 */
public record MailSettingsProblem(String field, String code) {
    public static final String REQUIRED = "required";
    public static final String OUT_OF_RANGE = "out_of_range";
    public static final String UNKNOWN_SECURITY = "unknown_security";
    public static final String CREDENTIALS_GO_TOGETHER = "credentials_go_together";
    public static final String INVALID_ADDRESS = "invalid_address";
    public static final String INVALID_URL = "invalid_url";
}
