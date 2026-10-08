package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.UpdateMyProfileUseCase;
import com.nido.api.identity.domain.model.EmailAddress;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.ProfileUpdate;
import com.nido.api.identity.domain.model.UpdateProfileCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountRecoveryPort;
import com.nido.api.identity.domain.port.out.AddressChangeCodePort;
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
    private final AddressChangeCodePort addressChangeCode;

    public UpdateMyProfileHandler(UserRepository userRepository, UserCommandPort userCommandPort,
                                  PasswordCheckPort passwordCheck, ProfileMailPort profileMail,
                                  AccountRecoveryPort accountRecovery, AddressChangeCodePort addressChangeCode) {
        this.userRepository = userRepository;
        this.userCommandPort = userCommandPort;
        this.passwordCheck = passwordCheck;
        this.profileMail = profileMail;
        this.accountRecovery = accountRecovery;
        this.addressChangeCode = addressChangeCode;
    }

    /**
     * Since "forgot password" exists, the address is how an account is recovered. Changing it on a
     * session alone would turn a borrowed or stolen session into the account itself: change the
     * address, ask for a reset, choose a password. So a new address asks for the current password, and
     * the previous address is told — it is the one the holder still reads if the change was not theirs —
     * and the reset links it was sent stop working. A change of letter case only is the same mailbox,
     * and asks for nothing.
     *
     * <p>When the account's second factor is the mail, the new address must prove it receives mail first: a
     * typo would otherwise lock its holder out at the next sign-in. The first request sends a code there and
     * saves nothing; the same request with the code saves. A taken address is refused before anything is sent —
     * the code would land in someone else's mailbox. A wrong code is counted next to it, in this transaction, so
     * that error does not roll the count back.
     */
    @Override
    @Transactional(noRollbackFor = IdentityException.EmailCodeInvalid.class)
    public ProfileUpdate updateProfile(UpdateProfileCommand command) {
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
            if (addressChangeCode.required(user.id())) {
                boolean taken = userRepository.findByEmail(new EmailAddress(command.email()))
                    .filter(other -> !other.id().equals(user.id()))
                    .isPresent();
                if (taken) {
                    throw new IdentityException.EmailAlreadyExists();
                }
                if (command.emailCode() == null) {
                    return new ProfileUpdate.EmailCodeSent(command.email(), addressChangeCode.send(user.id(), command.email()));
                }
                if (!addressChangeCode.check(user.id(), command.email(), command.emailCode())) {
                    throw new IdentityException.EmailCodeInvalid();
                }
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
        return new ProfileUpdate.Saved();
    }
}
