package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.port.in.DisableTotpUseCase;
import com.synapsys.api.mfa.domain.model.*;
import com.synapsys.api.mfa.application.dto.*;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class DisableTotpHandler implements DisableTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final UserTotpLifecyclePort userTotpLifecyclePort;
    private final TotpCodeValidatorPort codeValidator;
    private final TotpCodeReplayPort codeReplay;

    public DisableTotpHandler(UserTotpQueryPort userTotpQuery,
                              UserTotpLifecyclePort userTotpLifecyclePort,
                              TotpCodeValidatorPort codeValidator,
                              TotpCodeReplayPort codeReplay) {
        this.userTotpQuery = userTotpQuery;
        this.userTotpLifecyclePort = userTotpLifecyclePort;
        this.codeValidator = codeValidator;
        this.codeReplay = codeReplay;
    }

    @Override
    @Transactional
    public void disable(DisableTotpCommand command) {
        UserTotpProfile user = userTotpQuery.findById(command.userId())
            .orElseThrow(MfaException.UserNotFound::new);

        if (!user.totpEnabled()) throw new MfaException.TotpNotEnabled();
        String secret = user.totpSecret().orElseThrow(MfaException.TotpSetupNotStarted::new);
        if (!codeValidator.isValid(secret, command.code())) throw new MfaException.TotpCodeInvalid();
        if (!codeReplay.markCodeUsedIfAbsent(command.userId(), command.code())) throw new MfaException.TotpCodeInvalid();

        userTotpLifecyclePort.disableTotp(command.userId());
    }
}