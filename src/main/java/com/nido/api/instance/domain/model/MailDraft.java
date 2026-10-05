package com.nido.api.instance.domain.model;

/** A mail configuration to check or to try, as the settings hold it. */
public record MailDraft(String host, String port, String security, String username, String password,
                        String from, String publicUrl) {

    public static MailDraft of(EffectiveSettings settings) {
        return new MailDraft(
            settings.text(SettingKey.MAIL_HOST).orElse(null),
            settings.text(SettingKey.MAIL_PORT).orElse(null),
            settings.text(SettingKey.MAIL_SECURITY).orElse(null),
            settings.text(SettingKey.MAIL_USERNAME).orElse(null),
            settings.text(SettingKey.MAIL_PASSWORD).orElse(null),
            settings.text(SettingKey.MAIL_FROM).orElse(null),
            settings.text(SettingKey.PUBLIC_URL).orElse(null));
    }

    @Override
    public String toString() {
        return "MailDraft[host=" + host + ", port=" + port + ", security=" + security + ", username=" + username
            + ", password=" + (password == null ? "" : "***") + ", from=" + from + ", publicUrl=" + publicUrl + "]";
    }
}
