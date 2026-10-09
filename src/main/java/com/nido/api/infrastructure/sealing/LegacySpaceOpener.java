package com.nido.api.infrastructure.sealing;

import com.nido.api.infrastructure.encryption.LegacyDecryptor;

import java.util.Objects;
import java.util.UUID;

/**
 * Reads what a space stored before 0.16.0 — a value sealed with the legacy key ({@value #PREFIX}) or, before 0.14.0, a
 * value encrypted without an envelope — for the migration and the key check only. Never writes: the migration seals
 * what it reads with the current key (SpaceSealer). Goes with the legacy key — see EncryptionBackfillRunner.
 */
public final class LegacySpaceOpener {

    static final String PREFIX = "v2:";

    private final LegacyDecryptor decryptor;

    private LegacySpaceOpener(LegacyDecryptor decryptor) {
        this.decryptor = decryptor;
    }

    public static LegacySpaceOpener of(LegacyDecryptor decryptor) {
        return new LegacySpaceOpener(Objects.requireNonNull(decryptor, "decryptor"));
    }

    /**
     * The value, checked to belong to this place when it was sealed; a value without an envelope cannot say. Public for
     * the space module's configuration test; called by the migration only.
     */
    public String open(SealedColumn column, UUID rowId, String stored) {
        boolean sealed = stored.startsWith(PREFIX);
        String decrypted;
        try {
            decrypted = decryptor.decrypt(sealed ? stored.substring(PREFIX.length()) : stored);
        } catch (RuntimeException e) {
            throw new SealedValueRejected(column, rowId, SealedValueRejected.Reason.UNDECRYPTABLE, null);
        }
        if (!sealed) {
            return decrypted;
        }
        try {
            return Envelope.unwrap(column.referenceFor(rowId), decrypted);
        } catch (EnvelopeRejected rejected) {
            throw SpaceSealer.rejected(column, rowId, rejected);
        }
    }

    /** Whether the legacy key opens it at all, wherever it belongs: what the key check asks. */
    boolean decrypts(String stored) {
        try {
            decryptor.decrypt(stored.startsWith(PREFIX) ? stored.substring(PREFIX.length()) : stored);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
