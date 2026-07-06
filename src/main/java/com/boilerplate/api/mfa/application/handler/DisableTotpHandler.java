package com.boilerplate.api.mfa.application.handler;

import com.boilerplate.api.mfa.application.dto.DisableTotpCommand;
import com.boilerplate.api.mfa.application.port.in.DisableTotpUseCase;
import com.boilerplate.api.mfa.domain.model.MfaException;
import com.boilerplate.api.mfa.domain.model.UserTotpProfile;
import com.boilerplate.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.boilerplate.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.boilerplate.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.boilerplate.api.mfa.domain.port.out.UserTotpQueryPort;
import com.boilerplate.api.mfa.domain.model.*;
import com.boilerplate.api.mfa.application.dto.*;
import com.boilerplate.api.mfa.domain.port.out.*;
import com.boilerplate.api.shared.annotation.ApplicationService;
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