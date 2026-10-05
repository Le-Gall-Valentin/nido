package com.nido.api.mail.domain.port.out;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.RenderedMail;

import java.util.Locale;

public interface MailRendererPort {
    /** Writes the mail, its links starting with {@code appUrl}; throws on a missing message, value or subject. */
    RenderedMail render(MailContent content, Locale locale, String appUrl);
}
