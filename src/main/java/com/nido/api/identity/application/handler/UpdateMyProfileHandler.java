package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.UpdateMyProfileUseCase;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.UpdateProfileCommand;
import com.nido.api.identity.domain.model.User;
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

    public UpdateMyProfileHandler(UserRepository userRepository, UserCommandPort userCommandPort,
                                  PasswordCheckPort passwordCheck, ProfileMailPort profileMail) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
        this.passwordCheck = passwordCheck;
        this.profileMail = profileMail;
    }

    /**
     * Since "forgot password" exists, the address is how an account is recovered. Changing it on a
     * session alone would turn a borrowed or stolen session into the account itself: change the
     * address, ask for a reset, choose a password. So a new address asks for the current password, and
     * the previous address is told — it is the one the holder still reads if the change was not theirs.
     * A change of letter case only is the same mailbox, and asks for nothing.
     */
    @Override
    @Transactional
    public void updateProfile(UpdateProfileCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!user.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        boolean addressChanges = user.email() == null || !user.email().equalsIgnoreCase(command.email());
        if (addressChanges) {
            if (command.currentPassword() == null || command.currentPassword().isBlank()) {
                throw new IdentityException.CurrentPasswordRequired();
            }
            if (!passwordCheck.matches(user.id(), command.currentPassword())) {
                throw new IdentityException.InvalidCurrentPassword();
            }
        }
        userCommandPort.updateProfile(command);
        if (addressChanges && user.email() != null) {
            profileMail.emailChanged(command.username(), user.email(), command.email(), user.language());
        }
    }
}