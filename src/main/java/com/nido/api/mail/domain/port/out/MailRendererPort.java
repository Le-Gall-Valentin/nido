package com.nido.api.mail.domain.port.out;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.RenderedMail;

import java.util.Locale;

public interface MailRendererPort {
    /** Writes the mail, or throws: a missing message, a missing value or a missing subject is an error. */
    RenderedMail render(MailContent content, Locale locale);
}
