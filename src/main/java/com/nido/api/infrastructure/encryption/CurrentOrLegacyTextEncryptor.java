package com.nido.api.infrastructure.encryption;

import com.nido.api.shared.security.EncryptionKey;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.function.BooleanSupplier;

/**
 * The encryptor of the values encrypted under a key of their own rather than a space's — two-factor secrets, the SMTP
 * password, the mail queue. Writes with the current key only, behind {@value #PREFIX}; reads that, and reads what has no
 * prefix — what versions up to 0.15.x wrote — with the legacy key.
 *
 * <p>Why read both: the setting store is read at start by InstanceStartup, in no set order with the migration that
 * brings these values to the current key (EncryptionBackfillRunner). Once a start has brought every value to the
 * current format, {@code legacyAccepted} turns false and the fallback is closed: a value without the prefix is refused,
 * for nothing legitimate can be in that format any more (see LegacyFormats). A {@value #PREFIX} value that does not open
 * is refused too: it is never tried with the legacy key.
 */
public final class CurrentOrLegacyTextEncryptor implements TextEncryptor {

    public static final String PREFIX = "k2:";

    private final TextEncryptor current;
    private final LegacyDecryptor legacy;
    private final BooleanSupplier legacyAccepted;

    CurrentOrLegacyTextEncryptor(TextEncryptor current, LegacyDecryptor legacy, BooleanSupplier legacyAccepted) {
        this.current = current;
        this.legacy = legacy;
        this.legacyAccepted = legacyAccepted;
    }

    /** @param legacyAccepted asked only for a value without the prefix: whether earlier formats are still taken */
    public static CurrentOrLegacyTextEncryptor of(EncryptionKey key, String hexSalt, BooleanSupplier legacyAccepted) {
        return new CurrentOrLegacyTextEncryptor(DataKeys.current(key, hexSalt), DataKeys.legacy(key, hexSalt), legacyAccepted);
    }

    public static boolean isCurrent(String stored) {
        return stored.startsWith(PREFIX);
    }

    @Override
    public String encrypt(String text) {
        return PREFIX + current.encrypt(text);
    }

    @Override
    public String decrypt(String stored) {
        if (isCurrent(stored)) {
            return current.decrypt(stored.substring(PREFIX.length()));
        }
        if (!legacyAccepted.getAsBoolean()) {
            throw new IllegalStateException("A value of a format before 0.16.0 is refused: every value was brought to the "
                + "current one, so this one was written since by someone with access to the database");
        }
        return legacy.decrypt(stored);
    }
}
