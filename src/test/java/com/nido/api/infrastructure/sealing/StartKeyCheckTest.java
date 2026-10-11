package com.nido.api.infrastructure.sealing;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartKeyCheckTest {

    private final ExistingCiphertextCheck finance = mock(ExistingCiphertextCheck.class);
    private final ExistingCiphertextCheck secrets = mock(ExistingCiphertextCheck.class);
    private final StartKey knownKey = mock(StartKey.class);
    private final StartKey unconfirmedKey = mock(StartKey.class);

    private StartKeyCheck check(StartKey key) {
        return new StartKeyCheck(List.of(finance, secrets), key);
    }

    @Test
    void a_key_the_data_has_yet_to_confirm_is_confirmed_once_every_check_opened_with_it() {
        when(unconfirmedKey.awaitsConfirmation()).thenReturn(true);

        check(unconfirmedKey).verify();

        InOrder order = inOrder(finance, secrets, unconfirmedKey);
        order.verify(finance).verify();
        order.verify(secrets).verify();
        order.verify(unconfirmedKey).confirm();
        verify(unconfirmedKey, never()).forget();
    }

    @Test
    void a_key_confirmed_already_is_neither_confirmed_again_nor_taken_back() {
        check(knownKey).verify();

        verify(knownKey, never()).confirm();
        verify(knownKey, never()).forget();
    }

    @Test
    void an_unconfirmed_key_that_does_not_decrypt_has_its_fingerprint_taken_back_so_the_right_key_can_start_next() {
        when(unconfirmedKey.awaitsConfirmation()).thenReturn(true);
        doThrow(new IllegalStateException("does not decrypt")).when(secrets).verify();

        assertThatThrownBy(check(unconfirmedKey)::verify).hasMessageContaining("does not decrypt");

        verify(unconfirmedKey).forget();
        verify(unconfirmedKey, never()).confirm();
    }

    @Test
    void a_known_key_that_does_not_decrypt_keeps_its_fingerprint() {
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(check(knownKey)::verify).hasMessageContaining("does not decrypt");

        verify(knownKey, never()).forget();
        verify(secrets, never()).verify();
    }
}
