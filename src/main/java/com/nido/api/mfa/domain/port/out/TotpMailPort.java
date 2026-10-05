package com.nido.api.mfa.domain.port.out;

import java.util.UUID;

/**
 * What an account's holder is told when they turn their second factor on or off themselves — in case it was
 * not them. Mails nobody can switch off. An administrator's reset is identity's mail, not one of these.
 */
public interface TotpMailPort {

    void totpEnabled(UUID userId);

    void totpDisabled(UUID userId);
}
