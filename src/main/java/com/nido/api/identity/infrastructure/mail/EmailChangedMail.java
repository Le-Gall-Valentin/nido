package com.nido.api.identity.infrastructure.mail;

import com.nido.api.mail.domain.model.MailContent;

/**
 * mail/identity/email-changed.html. The new address is shown masked: enough for the holder to
 * recognise it, not enough to hand it out if the old mailbox is no longer theirs.
 */
public record EmailChangedMail(String username, String maskedNewAddress) implements MailContent {

    @Override
    public String template() {
        return "identity/email-changed";
    }

    /** {@code jane.doe@example.fr} → {@code ja•••@example.fr}; one letter kept for a local part of two or fewer. */
    static String mask(String address) {
        int at = address.indexOf('@');
        if (at < 1) {
            return "•••";
        }
        int kept = at <= 2 ? 1 : 2;
        return address.substring(0, kept) + "•••" + address.substring(at);
    }
}
