package com.nido.api.mfa.domain.port.out;

/** How a mail code, and what it is bound to, are kept: never in clear, and never checkable from the database alone. */
public interface MailCodeHasherPort {

    String codeHash(String binding, String code);

    /** Tells a resend for the same thing from a new request. */
    String bindingHash(String binding);

    /** Compared in constant time. */
    boolean matches(String codeHash, String binding, String code);
}
