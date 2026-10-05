package com.nido.api.mail.infrastructure.config;

import java.util.Optional;

/**
 * The mail settings of the moment, checked — for the adapters that talk to an SMTP server. Inside the
 * mail infrastructure only: the rest of the application asks MailConfigurationPort whether mail is on.
 */
@FunctionalInterface
public interface MailSettingsSource {

    /** Empty while mail is off, or while its configuration does not hold. */
    Optional<MailSettings> settings();
}
