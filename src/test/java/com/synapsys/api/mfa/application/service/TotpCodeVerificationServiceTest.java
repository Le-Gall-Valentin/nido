package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.application.dto.TotpCodeVerifyResult;
import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.synapsys.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotpCodeVerificationServiceTest {

    @Mock UserTotpQueryPort userTotpQuery;
    @Mock TotpCodeValidatorPort codeValidator;
    @Mock TotpCodeReplayPort codeReplay;

    private TotpCodeVerificationService service;

    private final UUID userId = UUID.randomUUID();
    private final String secret = "MYSECRET";

    @BeforeEach
    void setUp() {
        service = new TotpCodeVerificationService(userTotpQuery, codeValidator, codeReplay);
    }

    @Test
    void verifyAndConsume_validCode_returnsSuccess() {
        UserTotpProfile profile = new UserTotpProfile(userId, true, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(true);

        assertThat(service.verifyAndConsume(userId, "123456")).isEqualTo(TotpCodeVerifyResult.SUCCESS);
    }

    @Test
    void verifyAndConsume_userNotFound_returnsInvalid() {
        when(userTotpQuery.findById(userId)).thenReturn(Optional.empty());

        assertThat(service.verifyAndConsume(userId, "123456")).isEqualTo(TotpCodeVerifyResult.INVALID);
    }

    @Test
    void verifyAndConsume_secretNull_returnsInvalid() {
        UserTotpProfile profile = new UserTotpProfile(userId, false, Optional.empty());
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThat(service.verifyAndConsume(userId, "123456")).isEqualTo(TotpCodeVerifyResult.INVALID);
    }

    @Test
    void verifyAndConsume_invalidCode_returnsInvalid() {
        UserTotpProfile profile = new UserTotpProfile(userId, true, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "000000")).thenReturn(false);

        assertThat(service.verifyAndConsume(userId, "000000")).isEqualTo(TotpCodeVerifyResult.INVALID);
    }

    @Test
    void verifyAndConsume_totpDisabled_withExistingSecret_returnsInvalid() {
        // Enrollment started (secret saved) but not yet confirmed (totpEnabled=false)
        UserTotpProfile profile = new UserTotpProfile(userId, false, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));

        assertThat(service.verifyAndConsume(userId, "123456")).isEqualTo(TotpCodeVerifyResult.INVALID);
        verifyNoInteractions(codeValidator, codeReplay);
    }

    @Test
    void verifyAndConsume_replayedCode_returnsReplayed() {
        UserTotpProfile profile = new UserTotpProfile(userId, true, Optional.of(secret));
        when(userTotpQuery.findById(userId)).thenReturn(Optional.of(profile));
        when(codeValidator.isValid(secret, "123456")).thenReturn(true);
        when(codeReplay.markCodeUsedIfAbsent(userId, "123456")).thenReturn(false);

        assertThat(service.verifyAndConsume(userId, "123456")).isEqualTo(TotpCodeVerifyResult.REPLAYED);
    }
}