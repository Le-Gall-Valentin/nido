package com.synapsys.api.mfa.application.handler;

import com.synapsys.api.mfa.application.port.in.ConfirmTotpUseCase;
import com.synapsys.api.mfa.domain.model.*;
import com.synapsys.api.mfa.application.dto.*;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.model.TotpPolicy;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ConfirmTotpHandler implements ConfirmTotpUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final TotpCodeValidatorPort codeValidator;
    private final UserTotpLifecyclePort userTotpLifecyclePort;
    private final TotpCodeReplayPort codeReplay;
    private final TotpConfirmAttemptPort confirmAttemptPort;
    private final UserTotpSetupPort userTotpSetupPort;

    public ConfirmTotpHandler(UserTotpQueryPort userTotpQuery,
                              TotpCodeValidatorPort codeValidator,
                              UserTotpLifecyclePort userTotpLifecyclePort,
                              TotpCodeReplayPort codeReplay,
                              TotpConfirmAttemptPort confirmAttemptPort,
                              UserTotpSetupPort userTotpSetupPort) {
        this.userTotpQuery = userTotpQuery;
        this.codeValidator = codeValidator;
        this.userTotpLifecyclePort = userTotpLifecyclePort;
        this.codeReplay = codeReplay;
        this.confirmAttemptPort = confirmAttemptPort;
        this.userTotpSetupPort = userTotpSetupPort;
    }

    @Override
    @Transactional
    public void confirm(ConfirmTotpCommand command) {
        UserTotpProfile user = userTotpQuery.findById(command.userId())
            .orElseThrow(MfaException.UserNotFound::new);

        if (user.totpEnabled()) throw new MfaException.TotpAlreadyEnabled();
        String secret = user.totpSecret().orElseThrow(MfaException.TotpSetupNotStarted::new);

        if (!codeValidator.isValid(secret, command.code())) {
            int attempts = confirmAttemptPort.incrementAndGetAttempts(command.userId());
            if (attempts >= TotpPolicy.MAX_ATTEMPTS) {
                userTotpSetupPort.clearPendingSecret(command.userId());
                confirmAttemptPort.clearAttempts(command.userId());
                throw new MfaException.TotpConfirmMaxAttemptsExceeded();
            }
            throw new MfaException.TotpCodeInvalid();
        }

        if (!codeReplay.markCodeUsedIfAbsent(command.userId(), command.code())) {
            throw new MfaException.TotpCodeInvalid();
        }

        confirmAttemptPort.clearAttempts(command.userId());
        userTotpLifecyclePort.enableTotp(command.userId());
    }
}