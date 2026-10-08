package com.nido.api.mfa.infrastructure.security;

import com.nido.api.mfa.domain.port.out.MailCodeHasherPort;
import com.nido.api.shared.security.EncryptionKey;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Mail codes and their bindings as HMAC-SHA-256 under a key of their own, derived from the installation's master
 * key. Six digits are found in a moment from any hash the database alone can recompute — and a code is most often
 * bound to the account id, which the database holds. The master key lives outside the database: a dump gives
 * neither the codes nor a way to test a guess. JDK primitives only; the derived key never leaves the heap.
 */
@Component
public class HmacMailCodeHasherAdapter implements MailCodeHasherPort {

    private static final String ALGORITHM = "HmacSHA256";
    private static final HexFormat HEX = HexFormat.of();

    private final SecretKeySpec key;

    public HmacMailCodeHasherAdapter(EncryptionKey encryptionKey) {
        SecretKeySpec master = new SecretKeySpec(encryptionKey.value().getBytes(StandardCharsets.UTF_8), ALGORITHM);
        this.key = new SecretKeySpec(hmac(master, "nido/mfa/mail-codes"), ALGORITHM);
    }

    @Override
    public String codeHash(String binding, String code) {
        return HEX.formatHex(hmac(key, "code\0" + binding + "\0" + code));
    }

    @Override
    public String bindingHash(String binding) {
        return HEX.formatHex(hmac(key, "binding\0" + binding));
    }

    @Override
    public boolean matches(String codeHash, String binding, String code) {
        return MessageDigest.isEqual(
            codeHash.getBytes(StandardCharsets.US_ASCII),
            codeHash(binding, code).getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] hmac(SecretKeySpec key, String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
