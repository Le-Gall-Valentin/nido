package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.port.in.SetupTotpUseCase;
import com.synapsys.api.mfa.domain.model.*;
import com.synapsys.api.mfa.application.dto.*;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
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