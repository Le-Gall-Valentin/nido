package com.nido.api.identity.infrastructure.security;

import com.nido.api.mfa.application.port.in.GetTwoFactorMethodsUseCase;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MfaTwoFactorMethodsAdapterTest {

    @Test
    void the_methods_are_mfa_s() {
        GetTwoFactorMethodsUseCase methods = mock(GetTwoFactorMethodsUseCase.class);
        UUID jane = UUID.randomUUID();
        when(methods.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));

        assertThat(new MfaTwoFactorMethodsAdapter(methods).activeMethods(jane)).containsExactly(TwoFactorMethod.MAIL);
    }
}
