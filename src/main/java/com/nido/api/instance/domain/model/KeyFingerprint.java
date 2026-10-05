package com.nido.api.instance.domain.model;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * What the database remembers of the encryption key: enough to recognise it, never enough to recover it.
 *
 * <p>600 000 iterations of PBKDF2, and that number is the point. The data itself is encrypted with keys
 * derived in 1 024 iterations, so a fast fingerprint — one HMAC — would hand anyone with a dump a way to
 * test guesses against a weak, hand-picked key 600 times faster than the data allows. At this cost a
 * guess is dearer here than anywhere else in the database: the fingerprint opens no shortcut. It is paid
 * once per start, around 0.3 s.
 */
public record KeyFingerprint(byte[] hash, byte[] salt) {

    public static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static KeyFingerprint of(String key) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return of(key, salt, ITERATIONS);
    }

    static KeyFingerprint of(String key, byte[] salt, int iterations) {
        return new KeyFingerprint(derive(key, salt, iterations), salt.clone());
    }

    public boolean matches(String key) {
        return matches(key, ITERATIONS);
    }

    boolean matches(String key, int iterations) {
        return MessageDigest.isEqual(derive(key, salt, iterations), hash);
    }

    private static byte[] derive(String key, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is not available in this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof KeyFingerprint that && Arrays.equals(hash, that.hash) && Arrays.equals(salt, that.salt);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(hash) + Arrays.hashCode(salt);
    }

    @Override
    public String toString() {
        return "KeyFingerprint[***]";
    }
}
