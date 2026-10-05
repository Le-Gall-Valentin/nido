package com.nido.api.mail.domain.model;

/**
 * A mail configuration as another context holds it, before the mail context checks it. Port and
 * security may be null: their defaults (587, STARTTLS) apply.
 */
public record MailSettingsInput(String host, Integer port, String security, String username, String password,
                                String from, String appUrl) {
    @Override
    public String toString() {
        return "MailSettingsInput[host=" + host + ", port=" + port + ", security=" + security + ", username=" + username
            + ", password=" + (password == null || password.isEmpty() ? "" : "***") + ", from=" + from + ", appUrl=" + appUrl + "]";
    }
}
