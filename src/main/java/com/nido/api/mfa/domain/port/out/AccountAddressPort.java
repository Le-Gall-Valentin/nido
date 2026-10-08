package com.nido.api.mfa.domain.port.out;

import java.util.Optional;
import java.util.UUID;

/** The address an account has now — identity's, read when it is needed. */
public interface AccountAddressPort {

    /** Empty for an unknown account, or an anonymised one. */
    Optional<String> addressOf(UUID userId);
}
