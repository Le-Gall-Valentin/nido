package com.nido.api.infrastructure.sealing;

import com.nido.api.infrastructure.encryption.DataKeys;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacySpaceOpenerTest {

    private static final String MASTER = "test-encryption-secret-32chars!!";
    private static final String SALT = "00112233445566778899aabbccddeeff";
    private static final LegacySpaceOpener OPENER = LegacySpaceOpener.of(DataKeys.legacy(new EncryptionKey(MASTER), SALT));
    private static final SealedColumn AMOUNT = SealedColumn.ofSpace("finance_transactions", "amount_encrypted");

    private final UUID rent = UUID.randomUUID();

    @Test
    void a_value_sealed_by_0_14_to_0_15_opens_where_it_was_sealed() {
        String stored = LegacyKeys.sealedV2(MASTER, SALT, AMOUNT, rent, "850.00");

        assertThat(OPENER.open(AMOUNT, rent, stored)).isEqualTo("850.00");
        assertThat(OPENER.decrypts(stored)).isTrue();
    }

    @Test
    void a_value_encrypted_without_an_envelope_before_0_14_opens_as_it_is() {
        String stored = LegacyKeys.writer(MASTER, SALT).encrypt("850.00");

        assertThat(OPENER.open(AMOUNT, rent, stored)).isEqualTo("850.00");
    }

    @Test
    void a_legacy_value_moved_to_another_row_is_refused_without_showing_it() {
        String stored = LegacyKeys.sealedV2(MASTER, SALT, AMOUNT, UUID.randomUUID(), "850.00");

        assertThatThrownBy(() -> OPENER.open(AMOUNT, rent, stored))
            .isInstanceOfSatisfying(SealedValueRejected.class, e -> {
                assertThat(e.reason()).isEqualTo(SealedValueRejected.Reason.ELSEWHERE);
                assertThat(e.getMessage()).doesNotContain("850.00");
            });
        assertThat(OPENER.decrypts(stored)).as("the key opens it: where it belongs is not the key check's business").isTrue();
    }

    @Test
    void a_value_of_another_key_or_of_the_current_key_does_not_open() {
        String otherKey = LegacyKeys.writer("another-encryption-secret-32chr!", SALT).encrypt("850.00");
        String current = SpaceSealer.of(DataKeys.current(new EncryptionKey(MASTER), SALT)).seal(AMOUNT, rent, "850.00");

        for (String stored : new String[] {otherKey, current, "not-a-ciphertext"}) {
            assertThatThrownBy(() -> OPENER.open(AMOUNT, rent, stored))
                .isInstanceOfSatisfying(SealedValueRejected.class, e -> assertThat(e.reason()).isEqualTo(SealedValueRejected.Reason.UNDECRYPTABLE));
            assertThat(OPENER.decrypts(stored)).isFalse();
        }
    }

    @Test
    void remembering_derives_each_space_once() {
        AtomicInteger derivations = new AtomicInteger();
        LegacySpaceOpeners remembered = LegacySpaceOpeners.remembering(space -> {
            derivations.incrementAndGet();
            return OPENER;
        });
        UUID space = UUID.randomUUID();

        remembered.forSpace(space);
        remembered.forSpace(space);
        remembered.forSpace(UUID.randomUUID());

        assertThat(derivations).hasValue(2);
    }
}
