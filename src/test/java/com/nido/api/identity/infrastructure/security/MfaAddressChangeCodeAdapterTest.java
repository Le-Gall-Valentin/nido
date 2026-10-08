package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.model.EmailCodeCheck;
import com.nido.api.mfa.application.port.in.AddressChangeCodeUseCase;
import com.nido.api.mfa.domain.model.CodeCheck;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MfaAddressChangeCodeAdapterTest {

    private final AddressChangeCodeUseCase codes = mock(AddressChangeCodeUseCase.class);
    private final MfaAddressChangeCodeAdapter adapter = new MfaAddressChangeCodeAdapter(codes);
    private final UUID jane = UUID.randomUUID();

    @Test
    void every_answer_is_mfa_s() {
        when(codes.required(jane)).thenReturn(true);
        when(codes.send(jane, "new@test.com")).thenReturn(60L);

        assertThat(adapter.required(jane)).isTrue();
        assertThat(adapter.send(jane, "new@test.com")).isEqualTo(60);
    }

    @Test
    void every_check_is_said_in_identity_s_words() {
        when(codes.check(jane, "new@test.com", "004213"))
            .thenReturn(CodeCheck.SUCCESS, CodeCheck.INVALID, CodeCheck.REPLAYED, CodeCheck.EXPIRED, CodeCheck.SPENT);

        assertThat(adapter.check(jane, "new@test.com", "004213")).isEqualTo(EmailCodeCheck.VALID);
        assertThat(adapter.check(jane, "new@test.com", "004213")).isEqualTo(EmailCodeCheck.INVALID);
        assertThat(adapter.check(jane, "new@test.com", "004213")).isEqualTo(EmailCodeCheck.INVALID);
        assertThat(adapter.check(jane, "new@test.com", "004213")).isEqualTo(EmailCodeCheck.EXPIRED);
        assertThat(adapter.check(jane, "new@test.com", "004213")).isEqualTo(EmailCodeCheck.SPENT);
    }
}
