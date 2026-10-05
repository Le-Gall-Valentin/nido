package com.nido.api.authentication.domain.port.out;

/** The sessions as the installation sets them — asked at each token and cookie, so a change applies at once. */
public interface SessionSettingsPort {

    /**
     * The longest life an access token can be given (the settings refuse more). A cut-off must outlive
     * any token it may have to reject, whatever the setting was when that token was issued.
     */
    int LONGEST_ACCESS_TOKEN_MINUTES = 1440;

    int accessTokenMinutes();

    boolean secureCookies();
}
