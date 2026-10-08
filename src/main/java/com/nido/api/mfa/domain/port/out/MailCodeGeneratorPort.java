package com.nido.api.mfa.domain.port.out;

public interface MailCodeGeneratorPort {

    /** Six digits, leading zeros kept. */
    String newCode();
}
