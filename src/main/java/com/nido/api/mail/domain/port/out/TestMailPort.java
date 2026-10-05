package com.nido.api.mail.domain.port.out;

import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.TestMailOutcome;

import java.util.Locale;

public interface TestMailPort {
    /** Sends now, outside the queue, with these settings — saved or not. */
    TestMailOutcome send(MailSettingsInput input, Recipient to, Locale locale);
}
