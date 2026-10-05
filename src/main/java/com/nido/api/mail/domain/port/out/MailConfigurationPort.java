package com.nido.api.mail.domain.port.out;

import com.nido.api.mail.domain.model.ActiveMail;

import java.util.Optional;

public interface MailConfigurationPort {
    /**
     * Present while mail is configured and its configuration holds. Asked at the moment of use: mail
     * can be switched on and off from the settings page, without a restart.
     */
    Optional<ActiveMail> active();
}
