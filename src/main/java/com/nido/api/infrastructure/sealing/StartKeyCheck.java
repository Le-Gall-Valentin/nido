package com.nido.api.infrastructure.sealing;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The key this start was given, checked against the data already encrypted (ExistingCiphertextCheck), and the verdict
 * passed on to it (StartKey). A key whose fingerprint the data has yet to confirm is only taken on the data's word: an
 * installation older than 0.12 never recorded one, and keeping the fingerprint of a wrong key would lock the right one
 * out — so it is confirmed once the data opens with it, and its fingerprint taken back when it does not.
 */
@Component
public class StartKeyCheck {

    private final List<ExistingCiphertextCheck> checks;
    private final StartKey key;

    public StartKeyCheck(List<ExistingCiphertextCheck> checks, StartKey key) {
        this.checks = List.copyOf(checks);
        this.key = key;
    }

    /** Whether the data has yet to confirm the key: then it is checked even with nothing to convert. */
    public boolean keyAwaitsConfirmation() {
        return key.awaitsConfirmation();
    }

    /** @throws IllegalStateException naming what does not decrypt — nothing was encrypted with the key then */
    public void verify() {
        boolean unconfirmed = key.awaitsConfirmation();
        try {
            checks.forEach(ExistingCiphertextCheck::verify);
        } catch (RuntimeException refused) {
            if (unconfirmed) {
                key.forget();
            }
            throw refused;
        }
        if (unconfirmed) {
            key.confirm();
        }
    }
}
