package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.MailCodeDelivery;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengePort;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengeStorePort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SendChallengeMailCodeHandlerTest {

    private final TwoFactorChallengeStorePort challenges = mock(TwoFactorChallengeStorePort.class);
    private final UserCredentialsPort credentials = mock(UserCredentialsPort.class);
    private final TwoFactorChallengePort secondFactor = mock(TwoFactorChallengePort.class);
    private final SendChallengeMailCodeHandler handler = new SendChallengeMailCodeHandler(challenges, credentials, secondFactor);
    private final UUID jane = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(challenges.resolveChallenge("c")).thenReturn(Optional.of(jane));
        when(credentials.findById(jane)).thenReturn(Optional.of(
            new UserCredentials(jane, "jane", "jane@example.fr", "hash", true, Role.USER, Instant.now(), null)));
        when(secondFactor.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP, TwoFactorMethod.MAIL));
        when(secondFactor.usableMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP, TwoFactorMethod.MAIL));
    }

    @Test
    void the_code_leaves_and_the_wait_before_another_is_given() {
        when(secondFactor.sendMailCode(jane, "c")).thenReturn(new MailCodeDelivery.Sent(60));

        assertThat(handler.send("c")).isEqualTo(60);
    }

    @Test
    void a_refusal_says_which_and_for_how_long() {
        when(secondFactor.sendMailCode(jane, "c")).thenReturn(new MailCodeDelivery.TooSoon(30), new MailCodeDelivery.LimitReached(420));

        assertThatThrownBy(() -> handler.send("c")).isInstanceOfSatisfying(AuthenticationException.MailCodeRefused.class,
            refused -> { assertThat(refused.tooSoon()).isTrue(); assertThat(refused.retryAfterSeconds()).isEqualTo(30); });
        assertThatThrownBy(() -> handler.send("c")).isInstanceOfSatisfying(AuthenticationException.MailCodeRefused.class,
            refused -> { assertThat(refused.tooSoon()).isFalse(); assertThat(refused.retryAfterSeconds()).isEqualTo(420); });
    }

    @Test
    void no_challenge_no_code() {
        when(challenges.resolveChallenge("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.send("gone")).isInstanceOf(AuthenticationException.TwoFactorChallengeExpired.class);
    }

    @Test
    void an_account_without_the_mail_on_or_with_it_paused_is_told_which() {
        when(secondFactor.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.APP));
        assertThatThrownBy(() -> handler.send("c")).isInstanceOf(AuthenticationException.MethodNotEnabled.class);

        when(secondFactor.activeMethods(jane)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));
        when(secondFactor.usableMethods(jane)).thenReturn(EnumSet.noneOf(TwoFactorMethod.class));
        assertThatThrownBy(() -> handler.send("c")).isInstanceOf(AuthenticationException.MethodUnavailable.class);
    }
}
