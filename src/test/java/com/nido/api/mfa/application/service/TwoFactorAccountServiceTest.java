package com.nido.api.mfa.application.service;

import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TwoFactorAccountServiceTest {

    private final TwoFactorMethodStorePort store = mock(TwoFactorMethodStorePort.class);
    private final TwoFactorAccountService service = new TwoFactorAccountService(store);
    private final UUID jane = UUID.randomUUID();

    @Test
    void the_methods_on_are_read_from_the_store() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(store.activeMethodsAmong(List.of(jane))).thenReturn(Map.of(jane, Set.of(TwoFactorMethod.MAIL)));

        assertThat(service.activeMethods(jane)).containsExactly(TwoFactorMethod.MAIL);
        assertThat(service.activeMethodsAmong(List.of(jane))).containsEntry(jane, Set.of(TwoFactorMethod.MAIL));
    }
}
