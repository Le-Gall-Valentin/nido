package com.nido.api.instance.domain.model;

/**
 * What is wrong with one setting. {@code code} is shared with the frontend, which words it in its
 * instance-settings entity ({@code problem.*}): keep the constants and those keys in step — a code the
 * pages do not know yet is worded as unknown, never shown raw.
 */
public record SettingProblem(SettingKey key, String code) {
    public static final String REQUIRED = "required";
    public static final String NOT_A_NUMBER = "not_a_number";
    public static final String OUT_OF_RANGE = "out_of_range";
    public static final String NOT_A_BOOLEAN = "not_a_boolean";
    public static final String UNKNOWN_SECURITY = "unknown_security";
    public static final String INVALID_URL = "invalid_url";
    public static final String INVALID_ADDRESS = "invalid_address";
    public static final String CREDENTIALS_GO_TOGETHER = "credentials_go_together";
    public static final String ACCESS_NOT_SHORTER_THAN_SESSION = "access_not_shorter_than_session";
    public static final String PUBLIC_URL_REQUIRED_BY_MAIL = "public_url_required_by_mail";
    public static final String PASSWORD_REQUIRED_FOR_NEW_SERVER = "password_required_for_new_server";
}
