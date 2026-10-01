package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.UpdateMyProfileUseCase;
import com.nido.api.identity.domain.model.EmailAddress;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.UpdateProfileCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountRecoveryPort;
import com.nido.api.identity.domain.port.out.PasswordCheckPort;
import com.nido.api.identity.domain.port.out.ProfileMailPort;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;


@ApplicationService
public class UpdateMyProfileHandler implements UpdateMyProfileUseCase {

    private final UserRepository userRepository;
    private final UserCommandPort userCommandPort;

    private final PasswordCheckPort passwordCheck;
    private final ProfileMailPort profileMail;
    private final AccountRecoveryPort accountRecovery;

    public UpdateMyProfileHandler(UserRepository userRepository, UserCommandPort userCommandPort,
                                  PasswordCheckPort passwordCheck, ProfileMailPort profileMail,
                                  AccountRecoveryPort accountRecovery) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
        this.passwordCheck = passwordCheck;
        this.profileMail = profileMail;
        this.accountRecovery = accountRecovery;
    }

    /**
     * Since "forgot password" exists, the address is how an account is recovered. Changing it on a
     * session alone would turn a borrowed or stolen session into the account itself: change the
     * address, ask for a reset, choose a password. So a new address asks for the current password, and
     * the previous address is told — it is the one the holder still reads if the change was not theirs —
     * and the reset links it was sent stop working. A change of letter case only is the same mailbox,
     * and asks for nothing.
     */
    @Override
    @Transactional
    public void updateProfile(UpdateProfileCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!user.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        boolean addressChanges = user.email() == null || !EmailAddress.normalize(user.email()).equals(command.email());
        if (addressChanges) {
            if (command.currentPassword() == null || command.currentPassword().isBlank()) {
                throw new IdentityException.CurrentPasswordRequired();
            }
            if (!passwordCheck.matches(user.id(), command.currentPassword())) {
                throw new IdentityException.InvalidCurrentPassword();
            }
        }
        userCommandPort.updateProfile(command);
        if (addressChanges) {
            // A reset link already sent went to the old address: whoever still reads it must not keep a
            // way in once the account has moved on.
            accountRecovery.forgetResetLinks(user.id());
        }
        if (addressChanges && user.email() != null) {
            profileMail.emailChanged(user.username(), user.email(), command.email(), user.language());
        }
    }
}