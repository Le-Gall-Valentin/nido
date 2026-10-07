package com.nido.api.instance.domain.port.out;

import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.KeyFingerprint;

import java.time.Instant;

public interface InstanceStatePort {

    InstanceState load();

    /** Records it unconfirmed — see {@link #confirmFingerprint} — in the caller's transaction. */
    void recordFingerprint(KeyFingerprint fingerprint, boolean generated);

    /** Marks it confirmed if it is still the one in place, in the caller's transaction. True when this call did. */
    boolean confirmFingerprint(KeyFingerprint fingerprint);

    /**
     * Erases it if it is still the one in place and nothing confirmed it, in the caller's transaction. True when this
     * call erased it.
     */
    boolean forgetFingerprint(KeyFingerprint fingerprint);

    /**
     * Ends the setup, in the caller's transaction. True when this call ended it; false when it was
     * already over — two browsers finishing at the same second get one true between them.
     */
    boolean markSetupCompleted(Instant at);
}
