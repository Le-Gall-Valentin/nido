package com.boilerplate.api.mfa.application.handler;

import com.boilerplate.api.mfa.application.dto.SetupTotpCommand;
import com.boilerplate.api.mfa.application.port.in.SetupTotpUseCase;
import com.boilerplate.api.mfa.domain.model.MfaException;
import com.boilerplate.api.mfa.domain.model.TotpSetupResult;
import com.boilerplate.api.mfa.domain.model.UserTotpProfile;
import com.boilerplate.api.mfa.domain.port.out.TotpSecretGeneratorPort;
import com.boilerplate.api.mfa.domain.port.out.TotpUriBuilderPort;
import com.boilerplate.api.mfa.domain.port.out.UserTotpQueryPort;
import com.boilerplate.api.mfa.domain.port.out.UserTotpSetupPort;
import com.boilerplate.api.mfa.domain.model.*;
import com.boilerplate.api.mfa.application.dto.*;
import com.boilerplate.api.mfa.domain.port.out.*;
import com.boilerplate.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class SetupTotpHandler implements SetupTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final TotpSecretGeneratorPort secretGenerator;
    private final TotpUriBuilderPort uriBuilder;
    private final UserTotpSetupPort userTotpSetupPort;

    public SetupTotpHandler(UserTotpQueryPort userTotpQuery,
                            TotpSecretGeneratorPort secretGenerator,
                            TotpUriBuilderPort uriBuilder,
                            UserTotpSetupPort userTotpSetupPort) {
        this.userTotpQuery = userTotpQuery;
        this.secretGenerator = secretGenerator;
        this.uriBuilder = uriBuilder;
        this.userTotpSetupPort = userTotpSetupPort;
    }

    @Override
    @Transactional
    public TotpSetupResult setup(SetupTotpCommand command) {
        UserTotpProfile user = userTotpQuery.findById(command.userId())
            .orElseThrow(MfaException.UserNotFound::new);

        if (user.totpEnabled()) throw new MfaException.TotpAlreadyEnabled();

        String candidate = secretGenerator.generateSecret();
        boolean saved = userTotpSetupPort.saveTotpSecretIfAbsent(command.userId(), candidate);

        if (!saved) {
            UserTotpProfile refreshed = userTotpQuery.findById(command.userId())
                .orElseThrow(MfaException.UserNotFound::new);
            if (refreshed.totpEnabled()) throw new MfaException.TotpAlreadyEnabled();
            String existing = refreshed.totpSecret().orElseThrow(MfaException.TotpSetupNotStarted::new);
            return new TotpSetupResult(existing, uriBuilder.buildOtpauthUri(existing, command.email()));
        }

        return new TotpSetupResult(candidate, uriBuilder.buildOtpauthUri(candidate, command.email()));
    }
}