package com.nido.api.identity.infrastructure.security;

import com.nido.api.mfa.application.port.in.AddressChangeCodeUseCase;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MfaAddressChangeCodeAdapterTest {

    @Test
    void every_answer_is_mfa_s() {
        AddressChangeCodeUseCase codes = mock(AddressChangeCodeUseCase.class);
        MfaAddressChangeCodeAdapter adapter = new MfaAddressChangeCodeAdapter(codes);
        UUID jane = UUID.randomUUID();
        when(codes.required(jane)).thenReturn(true);
        when(codes.send(jane, "new@test.com")).thenReturn(60L);
        when(codes.check(jane, "new@test.com", "004213")).thenReturn(true);

        assertThat(adapter.required(jane)).isTrue();
        assertThat(adapter.send(jane, "new@test.com")).isEqualTo(60);
        assertThat(adapter.check(jane, "new@test.com", "004213")).isTrue();
    }
}
