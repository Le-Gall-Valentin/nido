package com.nido.api.infrastructure.sealing;

import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Objects;
import java.util.UUID;

/**
 * Seals the values of one space: a value goes into its {@link Envelope} — the place it belongs to and a padding —
 * which the space's {@link TextEncryptor} encrypts, and is stored as {@code v2:} followed by the ciphertext.
 * Opening checks all of it and refuses anything else: a value moved to another row or column, a value of the format
 * before 0.14.0, a value altered in any byte. No cryptography of its own.
 */
public final class SpaceSealer {

    static final String PREFIX = "v2:";

    private final TextEncryptor encryptor;

    private SpaceSealer(TextEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    public static SpaceSealer of(TextEncryptor encryptor) {
        return new SpaceSealer(Objects.requireNonNull(encryptor, "encryptor"));
    }

    public String seal(SealedColumn column, UUID rowId, String value) {
        return PREFIX + encryptor.encrypt(Envelope.wrap(column.referenceFor(rowId), value));
    }

    public String sealNullable(SealedColumn column, UUID rowId, String value) {
        return value == null ? null : seal(column, rowId, value);
    }

    public String open(SealedColumn column, UUID rowId, String stored) {
        if (stored == null || !isSealed(stored)) {
            throw new SealedValueRejected(column, rowId, SealedValueRejected.Reason.NOT_SEALED, null);
        }
        String envelope;
        try {
            envelope = encryptor.decrypt(stored.substring(PREFIX.length()));
        } catch (RuntimeException e) {
            throw new SealedValueRejected(column, rowId, SealedValueRejected.Reason.UNDECRYPTABLE, null);
        }
        try {
            return Envelope.unwrap(column.referenceFor(rowId), envelope);
        } catch (EnvelopeRejected rejected) {
            throw rejected(column, rowId, rejected);
        }
    }

    /**
     * A place that is no place can only come from a text someone typed, encrypted in the format before 0.14.0: it is not
     * repeated in the log.
     */
    static SealedValueRejected rejected(SealedColumn column, UUID rowId, EnvelopeRejected rejected) {
        return rejected.claimedReference()
            .filter(SealedColumn::isReference)
            .map(claimed -> new SealedValueRejected(column, rowId, SealedValueRejected.Reason.ELSEWHERE, "sealed for " + claimed))
            .orElseGet(() -> new SealedValueRejected(column, rowId, SealedValueRejected.Reason.MALFORMED, null));
    }

    public String openNullable(SealedColumn column, UUID rowId, String stored) {
        return stored == null ? null : open(column, rowId, stored);
    }

    static boolean isSealed(String stored) {
        return stored.startsWith(PREFIX);
    }

    /** What versions before 0.14.0 stored: the bare value, encrypted without an envelope. For the migration only. */
    String openUnsealed(String stored) {
        return encryptor.decrypt(stored);
    }
}
