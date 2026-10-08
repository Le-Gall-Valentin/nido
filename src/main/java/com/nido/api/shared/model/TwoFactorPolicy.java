package com.nido.api.shared.model;

import java.time.Duration;

/** The numbers second factors live by — one place, read by mfa and authentication alike. */
public final class TwoFactorPolicy {

    /** Failed codes an account may give before verification locks for a while — both methods together. */
    public static final int MAX_ATTEMPTS = 5;

    /** How long a code sent by mail can be used, and how long its mail stays worth delivering. */
    public static final Duration CODE_VALIDITY = Duration.ofMinutes(10);

    /** How long before the same code can be sent again for the same thing. */
    public static final Duration RESEND_DELAY = Duration.ofSeconds(60);

    /** Mails of code an account can be sent per {@link #MAIL_SEND_WINDOW}, every purpose together. */
    public static final int MAIL_SENDS_PER_WINDOW = 5;

    public static final Duration MAIL_SEND_WINDOW = Duration.ofMinutes(15);

    private TwoFactorPolicy() {}
}
