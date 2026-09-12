package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.dto.SetupTotpCommand;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.model.TotpSetupResult;
import com.nido.api.mfa.domain.model.UserTotpProfile;
import com.nido.api.mfa.domain.port.out.TotpSecretGeneratorPort;
import com.nido.api.mfa.domain.port.out.TotpUriBuilderPort;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.mfa.domain.port.out.UserTotpQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupTotpHandlerTest {

    @Mock UserTotpQueryPort userTotpQuery;
    @Mock TotpSecretGeneratorPort secretGenerator;
    @Mock TotpUriBuilderPort uriBuilder;
    @Mock PendingTotpEnrolmentPort pendingEnrolment;

    private SetupTotpHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final String email = "user@test.com";

    @BeforeEach
    void setUp() {
        handler = new SetupTotpHandler(userTotpQuery, secretGenerator, uriBuilder, pendingEnrolment);
    }

    @Test
    void setup_userWithNoTotp_savesSecretAndReturnsResult() {
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(secretGenerator.generateSecret()).thenReturn("NEWSECRET");
        when(uriBuilder.buildOtpauthUri("NEWSECRET", email)).thenReturn("otpauth://totp/...");
        when(pendingEnrolment.startIfAbsent(userId, "NEWSECRET")).thenReturn(true);

        TotpSetupResult result = handler.setup(new SetupTotpCommand(userId, email));

        assertThat(result.secret()).isEqualTo("NEWSECRET");
        assertThat(result.otpauthUri()).isEqualTo("otpauth://totp/...");
    }

    @Test
    void setup_totpAlreadyEnabled_throwsTotpAlreadyEnabled() {
        UserTotpProfile profile = new UserTotpProfile(userId,true, Optional.of("EXISTING"));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> handler.setup(new SetupTotpCommand(userId, email)))
            .isInstanceOf(MfaException.TotpAlreadyEnabled.class);
    }

    @Test
    void setup_userNotFound_throwsUserNotFound() {
        when(userTotpQuery.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.setup(new SetupTotpCommand(userId, email)))
            .isInstanceOf(MfaException.UserNotFound.class);
    }

    @Test
    void setup_enrolmentVanishedBetweenTheTwoCalls_saysSetupHasNotStarted() {
        // startIfAbsent said someone got there first, but the enrolment expired before it could be
        // read back. Rare, and the honest answer is that there is nothing in progress: the caller
        // retries and gets a fresh one.
        UserTotpProfile profile = new UserTotpProfile(userId, false, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(secretGenerator.generateSecret()).thenReturn("CANDIDATE");
        when(pendingEnrolment.startIfAbsent(userId, "CANDIDATE")).thenReturn(false);
        when(pendingEnrolment.find(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.setup(new SetupTotpCommand(userId, email)))
            .isInstanceOf(MfaException.TotpSetupNotStarted.class);
    }

    @Test
    void setup_enrolmentAlreadyUnderWay_returnsTheOneBeingShown() {
        // Two tabs: the second must show the same QR code as the first, not a rival one.
        UserTotpProfile profile = new UserTotpProfile(userId,false, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(pendingEnrolment.find(userId)).thenReturn(Optional.of("EXISTING"));
        when(secretGenerator.generateSecret()).thenReturn("CANDIDATE");
        when(pendingEnrolment.startIfAbsent(userId, "CANDIDATE")).thenReturn(false);
        when(uriBuilder.buildOtpauthUri("EXISTING", email)).thenReturn("otpauth://totp/existing");

        TotpSetupResult result = handler.setup(new SetupTotpCommand(userId, email));

        assertThat(result.secret()).isEqualTo("EXISTING");
        assertThat(result.otpauthUri()).isEqualTo("otpauth://totp/existing");
    }
}