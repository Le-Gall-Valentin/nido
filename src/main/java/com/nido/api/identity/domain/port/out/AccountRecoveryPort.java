package com.nido.api.identity.domain.port.out;

import java.util.UUID;

/** How an account is recovered when its password is forgotten — which follows its address. */
public interface AccountRecoveryPort {

    /** Voids every reset link already sent: they went to an address the account no longer uses. */
    void forgetResetLinks(UUID userId);
}
