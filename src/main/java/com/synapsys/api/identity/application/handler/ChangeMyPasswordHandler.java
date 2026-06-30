package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.application.port.in.ChangeMyPasswordUseCase;
import com.synapsys.api.identity.domain.model.ChangeMyPasswordCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.CredentialChangePort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ChangeMyPasswordHandler implements ChangeMyPasswordUseCase {

    private final UserRepository userRepository;
    private final CredentialChangePort credentialChangePort;

    public ChangeMyPasswordHandler(UserRepository userRepository, CredentialChangePort credentialChangePort) {
        this.userRepository = userRepository;
        this.credentialChangePort = credentialChangePort;
    }

    @Override
    @Transactional
    public void changeMyPassword(ChangeMyPasswordCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!user.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        credentialChangePort.changePassword(command.userId(), command.currentPassword(), command.newPassword());
    }
}
