package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.model.EmailCodeCheck;
import com.nido.api.identity.domain.model.EmailCodeDelivery;
import com.nido.api.mfa.application.port.in.AddressChangeCodeUseCase;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaAddressChangeCodeAdapterTest {

    private final AddressChangeCodeUseCase codes = mock(AddressChangeCodeUseCase.class);
    private final MfaAddressChangeCodeAdapter adapter = new MfaAddressChangeCodeAdapter(codes);
    private final UUID jane = UUID.randomUUID();

    @Test
    void whether_the_mail_method_is_on_is_mfa_s_to_say() {
        when(codes.mailMethodOn(jane)).thenReturn(true);

        assertThat(adapter.mailMethodOn(jane)).isTrue();
    }

    @Test
    void every_answer_to_a_send_is_said_in_identity_s_words() {
        // mfa's refusals stay mfa's: identity words them itself, and never meets an MfaException.
        when(codes.send(jane, "new@test.com")).thenReturn(new CodeDelivery.Sent(60), new CodeDelivery.TooSoon(40),
            new CodeDelivery.LimitReached(420), new CodeDelivery.Unavailable());

        assertThat(adapter.send(jane, "new@test.com")).isEqualTo(new EmailCodeDelivery.Sent(60));
        assertThat(adapter.send(jane, "new@test.com")).isEqualTo(new EmailCodeDelivery.TooSoon(40));
        assertThat(adapter.send(jane, "new@test.com")).isEqualTo(new EmailCodeDelivery.LimitReached(420));
        assertThat(adapter.send(jane, "new@test.com")).isEqualTo(new EmailCodeDelivery.Unavailable());
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

    @Test
    void forgoing_the_mail_method_is_mfa_s_to_do() {
        adapter.forgoMailMethod(jane);

        verify(codes).forgoMailMethod(jane);
    }
}
