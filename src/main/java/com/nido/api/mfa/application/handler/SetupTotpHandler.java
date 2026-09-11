package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.dto.SetupTotpCommand;
import com.nido.api.mfa.application.port.in.SetupTotpUseCase;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.model.TotpSetupResult;
import com.nido.api.mfa.domain.model.UserTotpProfile;
import com.nido.api.mfa.domain.port.out.TotpSecretGeneratorPort;
import com.nido.api.mfa.domain.port.out.TotpUriBuilderPort;
import com.nido.api.mfa.domain.port.out.UserTotpQueryPort;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class SetupTotpHandler implements SetupTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final TotpSecretGeneratorPort secretGenerator;
    private final TotpUriBuilderPort uriBuilder;
    private final PendingTotpEnrolmentPort pendingEnrolment;

    public SetupTotpHandler(UserTotpQueryPort userTotpQuery,
                            TotpSecretGeneratorPort secretGenerator,
                            TotpUriBuilderPort uriBuilder,
                            PendingTotpEnrolmentPort pendingEnrolment) {
        this.userTotpQuery = userTotpQuery;
        this.secretGenerator = secretGenerator;
        this.uriBuilder = uriBuilder;
        this.pendingEnrolment = pendingEnrolment;
    }

    @Override
    @Transactional
    public TotpSetupResult setup(SetupTotpCommand command) {
        UserTotpProfile user = userTotpQuery.findById(command.userId())
            .orElseThrow(MfaException.UserNotFound::new);

        if (user.totpEnabled()) throw new MfaException.TotpAlreadyEnabled();

        String candidate = secretGenerator.generateSecret();
        if (pendingEnrolment.startIfAbsent(command.userId(), candidate)) {
            return new TotpSetupResult(candidate, uriBuilder.buildOtpauthUri(candidate, command.email()));
        }

        // An enrolment was already under way: hand back the one being shown rather than a second
        // QR code. It can still have expired between the two calls, in which case there is nothing
        // to hand back and the caller starts over.
        String existing = pendingEnrolment.find(command.userId())
            .orElseThrow(MfaException.TotpSetupNotStarted::new);
        return new TotpSetupResult(existing, uriBuilder.buildOtpauthUri(existing, command.email()));
    }
}