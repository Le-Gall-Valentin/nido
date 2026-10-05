package com.nido.api.instance.domain.model;

import java.util.Optional;

/**
 * The installation as the database knows it. {@code keyGenerated}: Nido made the key itself, so the
 * setup screen still has to show it once.
 */
public record InstanceState(Optional<KeyFingerprint> fingerprint, boolean keyGenerated, boolean setupCompleted) {}
