package com.nido.api.infrastructure.sealing;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * What is encrypted in place of a value: the value, the place it belongs to, and a padding.
 *
 * <pre>&lt;reference&gt;|&lt;n&gt;|&lt;value&gt;&lt;padding&gt;</pre>
 *
 * <p>The reference names the table, the column and the row, so a ciphertext moved elsewhere opens on a
 * reference that is not the one asked for. {@code n} is the value's length in UTF-16 units, so the value comes
 * back exactly whatever it holds; it is always written on {@value #LENGTH_DIGITS} digits, or its width would tell
 * what the padding hides. The padding, made of {@code #}, brings the value to the next power of two of at least
 * {@value #MIN_BUCKET} bytes of UTF-8: within a column, the stored length tells nothing but that power of two.
 *
 * <p>Plain text handling — no key, no cipher. The encryption is the space's AES-GCM key (DataKeys), whose GCM
 * authenticates all of this: the reference cannot be rewritten without the key.
 */
public final class Envelope {

    static final int MIN_BUCKET = 32;
    static final int LENGTH_DIGITS = 9;
    /** Below 10^{@value #LENGTH_DIGITS}: a value's length in UTF-16 units never exceeds its length in UTF-8 bytes. */
    private static final int MAX_BUCKET = 1 << 29;
    private static final char SEPARATOR = '|';
    private static final char PAD = '#';

    private Envelope() {}

    public static String wrap(String reference, String value) {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(value, "value");
        if (reference.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("A reference cannot hold '" + SEPARATOR + "'");
        }
        int bytes = utf8Length(value);
        int bucket = bucketFor(bytes);
        return reference + SEPARATOR + String.format("%0" + LENGTH_DIGITS + "d", value.length()) + SEPARATOR + value
            + String.valueOf(PAD).repeat(bucket - bytes);
    }

    /**
     * The value an envelope holds, provided it is one and it belongs where it is read.
     *
     * @throws EnvelopeRejected saying, for an envelope of another place, which place
     */
    public static String unwrap(String expectedReference, String text) {
        int first = text.indexOf(SEPARATOR);
        int second = first < 0 ? -1 : text.indexOf(SEPARATOR, first + 1);
        if (second < 0) {
            throw EnvelopeRejected.malformed();
        }
        String digits = text.substring(first + 1, second);
        if (digits.length() != LENGTH_DIGITS || digits.chars().anyMatch(c -> c < '0' || c > '9')) {
            throw EnvelopeRejected.malformed();
        }
        int length = Integer.parseInt(digits);
        int start = second + 1;
        if (length > text.length() - start) {
            throw EnvelopeRejected.malformed();
        }
        String value = text.substring(start, start + length);
        String padding = text.substring(start + length);
        int bytes = utf8Length(value);
        if (bytes > MAX_BUCKET || padding.length() != bucketFor(bytes) - bytes || padding.chars().anyMatch(c -> c != PAD)) {
            throw EnvelopeRejected.malformed();
        }
        String reference = text.substring(0, first);
        if (!reference.equals(expectedReference)) {
            throw EnvelopeRejected.elsewhere(reference);
        }
        return value;
    }

    static int bucketFor(int bytes) {
        if (bytes > MAX_BUCKET) {
            throw new IllegalArgumentException("A value of " + bytes + " bytes is too long to seal");
        }
        int bucket = MIN_BUCKET;
        while (bucket < bytes) {
            bucket <<= 1;
        }
        return bucket;
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
