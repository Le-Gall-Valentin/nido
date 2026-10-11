package com.nido.api.infrastructure.sealing;

import com.nido.api.infrastructure.encryption.DataKeys;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceSealerTest {

    private static final TextEncryptor KEY = DataKeys.current(new EncryptionKey("test-encryption-secret-32chars!!"), "00112233445566778899aabbccddeeff");
    private static final TextEncryptor OTHER_KEY = DataKeys.current(new EncryptionKey("another-encryption-secret-32chr!"), "00112233445566778899aabbccddeeff");
    private static final SealedColumn AMOUNT = SealedColumn.ofSpace("finance_transactions", "amount_encrypted");
    private static final SealedColumn LABEL = SealedColumn.ofSpace("finance_transactions", "label_encrypted");

    private final SpaceSealer sealer = SpaceSealer.of(KEY);
    private final UUID rent = UUID.randomUUID();
    private final UUID coffee = UUID.randomUUID();

    @Test
    void a_sealed_value_starts_with_v3_and_opens_where_it_was_sealed() {
        String stored = sealer.seal(AMOUNT, rent, "850.00");

        assertThat(stored).startsWith("v3:");
        assertThat(sealer.open(AMOUNT, rent, stored)).isEqualTo("850.00");
    }

    @Test
    void a_value_moved_to_another_row_or_column_is_refused() {
        String stored = sealer.seal(AMOUNT, rent, "850.00");

        assertThatThrownBy(() -> sealer.open(AMOUNT, coffee, stored))
            .isInstanceOfSatisfying(SealedValueRejected.class, e -> {
                assertThat(e.reason()).isEqualTo(SealedValueRejected.Reason.ELSEWHERE);
                assertThat(e.getMessage()).contains(coffee.toString()).doesNotContain("850.00");
            });
        assertThatThrownBy(() -> sealer.open(LABEL, rent, stored))
            .isInstanceOfSatisfying(SealedValueRejected.class, e -> assertThat(e.reason()).isEqualTo(SealedValueRejected.Reason.ELSEWHERE));
    }

    @Test
    void the_formats_before_0_16_and_a_value_of_another_key_are_refused() {
        String master = "test-encryption-secret-32chars!!";
        String salt = "00112233445566778899aabbccddeeff";
        for (String before : new String[] {LegacyKeys.sealedV2(master, salt, AMOUNT, rent, "850.00"),
                                           LegacyKeys.writer(master, salt).encrypt("850.00"), KEY.encrypt("850.00")}) {
            assertThatThrownBy(() -> sealer.open(AMOUNT, rent, before))
                .isInstanceOfSatisfying(SealedValueRejected.class, e -> assertThat(e.reason()).isEqualTo(SealedValueRejected.Reason.NOT_SEALED));
        }
        assertThatThrownBy(() -> sealer.open(AMOUNT, rent, SpaceSealer.of(OTHER_KEY).seal(AMOUNT, rent, "850.00")))
            .isInstanceOfSatisfying(SealedValueRejected.class, e -> assertThat(e.reason()).isEqualTo(SealedValueRejected.Reason.UNDECRYPTABLE));
    }

    @Test
    void a_claimed_place_that_is_no_place_never_reaches_the_log() {
        // A sealing never writes such a place: only a ciphertext forged with the key could claim it.
        String typed = SpaceSealer.PREFIX + KEY.encrypt(Envelope.wrap("Loyer\nERROR forged line", "850.00"));

        assertThatThrownBy(() -> sealer.open(AMOUNT, rent, typed))
            .isInstanceOfSatisfying(SealedValueRejected.class, e -> {
                assertThat(e.reason()).isEqualTo(SealedValueRejected.Reason.MALFORMED);
                assertThat(e.getMessage()).doesNotContain("forged");
            });
    }

    @Test
    void the_nullable_forms_keep_null_as_null() {
        assertThat(sealer.sealNullable(LABEL, rent, null)).isNull();
        assertThat(sealer.openNullable(LABEL, rent, null)).isNull();
    }

    @Test
    void two_amounts_stored_for_the_same_column_have_the_same_length() {
        assertThat(sealer.seal(AMOUNT, rent, "3.50")).hasSameSizeAs(sealer.seal(AMOUNT, coffee, "12500.00"));
    }
}
