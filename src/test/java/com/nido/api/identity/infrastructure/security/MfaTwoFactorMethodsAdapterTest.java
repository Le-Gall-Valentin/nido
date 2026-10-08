package com.nido.api.identity.infrastructure.security;

import com.nido.api.mfa.application.port.in.AdminRemoveTwoFactorMethodsUseCase;
import com.nido.api.mfa.application.port.in.DeleteTwoFactorDataUseCase;
import com.nido.api.mfa.application.port.in.GetTwoFactorMethodsUseCase;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaTwoFactorMethodsAdapterTest {

    @Test
    void the_methods_are_mfa_s() {
        GetTwoFactorMethodsUseCase methods = mock(GetTwoFactorMethodsUseCase.class);
        UUID jane = UUID.randomUUID();
        when(methods.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));

        assertThat(new MfaTwoFactorMethodsAdapter(methods, mock(AdminRemoveTwoFactorMethodsUseCase.class), mock(DeleteTwoFactorDataUseCase.class)).activeMethods(jane)).containsExactly(TwoFactorMethod.MAIL);
    }

    @Test
    void removing_and_erasing_are_mfa_s_too() {
        GetTwoFactorMethodsUseCase methods = mock(GetTwoFactorMethodsUseCase.class);
        AdminRemoveTwoFactorMethodsUseCase remove = mock(AdminRemoveTwoFactorMethodsUseCase.class);
        DeleteTwoFactorDataUseCase delete = mock(DeleteTwoFactorDataUseCase.class);
        MfaTwoFactorMethodsAdapter adapter = new MfaTwoFactorMethodsAdapter(methods, remove, delete);
        UUID jane = UUID.randomUUID();
        when(remove.remove(jane, Set.of(TwoFactorMethod.APP))).thenReturn(Set.of(TwoFactorMethod.APP));

        assertThat(adapter.removeByAdmin(jane, Set.of(TwoFactorMethod.APP))).containsExactly(TwoFactorMethod.APP);
        adapter.deleteAll(jane);
        verify(delete).deleteUserData(jane);
    }
}
