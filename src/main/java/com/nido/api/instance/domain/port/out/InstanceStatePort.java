package com.nido.api.instance.domain.port.out;

import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.KeyFingerprint;

import java.time.Instant;

public interface InstanceStatePort {

    InstanceState load();

    /** Joins the caller's transaction. */
    void recordFingerprint(KeyFingerprint fingerprint, boolean generated);

    /** Erases the fingerprint if it is still this one, in the caller's transaction. */
    void forgetFingerprint(KeyFingerprint fingerprint);

    /**
     * Ends the setup, in the caller's transaction. True when this call ended it; false when it was
     * already over — two browsers finishing at the same second get one true between them.
     */
    boolean markSetupCompleted(Instant at);
}
