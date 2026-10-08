package com.nido.api.mfa.application.service;

import com.nido.api.mfa.application.method.TwoFactorMethodHandler;
import com.nido.api.mfa.application.method.TwoFactorMethods;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwoFactorAccountServiceTest {

    private final TwoFactorMethodStorePort store = mock(TwoFactorMethodStorePort.class);
    private final TwoFactorMethodHandler app = mock(TwoFactorMethodHandler.class);
    private final TwoFactorMethodHandler mail = mock(TwoFactorMethodHandler.class);
    private final UUID jane = UUID.randomUUID();
    private TwoFactorAccountService service;

    @BeforeEach
    void setUp() {
        when(app.method()).thenReturn(TwoFactorMethod.APP);
        when(mail.method()).thenReturn(TwoFactorMethod.MAIL);
        service = new TwoFactorAccountService(store, new TwoFactorMethods(List.of(app, mail)));
    }

    @Test
    void the_methods_on_are_read_from_the_store() {
        when(store.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(store.activeMethodsAmong(List.of(jane))).thenReturn(Map.of(jane, Set.of(TwoFactorMethod.MAIL)));

        assertThat(service.activeMethods(jane)).containsExactly(TwoFactorMethod.MAIL);
        assertThat(service.activeMethodsAmong(List.of(jane))).containsEntry(jane, Set.of(TwoFactorMethod.MAIL));
    }

    @Test
    void an_administrator_removes_only_what_was_on_and_says_what() {
        when(store.disable(jane, TwoFactorMethod.APP)).thenReturn(true);
        when(store.disable(jane, TwoFactorMethod.MAIL)).thenReturn(false);

        assertThat(service.remove(jane, EnumSet.of(TwoFactorMethod.APP, TwoFactorMethod.MAIL)))
            .containsExactly(TwoFactorMethod.APP);
    }

    @Test
    void an_enrolment_half_finished_is_cleared_with_the_method_asked_for() {
        when(store.disable(jane, TwoFactorMethod.APP)).thenReturn(false);

        assertThat(service.remove(jane, EnumSet.of(TwoFactorMethod.APP))).isEmpty();
        verify(app).forgetPending(jane);
        verify(mail, never()).forgetPending(jane);
    }

    @Test
    void erasing_an_account_takes_its_methods_and_everything_under_way() {
        service.deleteUserData(jane);

        verify(app).forgetPending(jane);
        verify(mail).forgetPending(jane);
        verify(store).deleteAll(jane);
    }
}
