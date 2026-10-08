package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.port.in.UpdateMyProfileUseCase;
import com.nido.api.identity.domain.model.EmailAddress;
import com.nido.api.identity.domain.model.EmailCodeDelivery;
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

import java.util.Optional;


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
     * that error does not roll the count back. While mail is off nothing can prove the address: it is saved, and
     * the code by mail goes with the old one — kept, it would send every future code to an address nobody
     * proved, locking out a holder who mistyped it, or serving whoever changed it.
     */
    @Override
    @Transactional(noRollbackFor = {IdentityException.EmailCodeInvalid.class, IdentityException.EmailCodeSpent.class})
    public ProfileUpdate updateProfile(UpdateProfileCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!user.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        boolean addressChanges = user.email() == null || !EmailAddress.normalize(user.email()).equals(command.email());
        boolean forgoMailMethod = false;
        if (addressChanges) {
            checkCurrentPassword(user, command);
            if (addressChangeCode.mailMethodOn(user.id())) {
                refuseTakenAddress(user, command);
                if (command.emailCode() == null) {
                    Optional<ProfileUpdate> codeSent = sendCode(user, command);
                    if (codeSent.isPresent()) {
                        return codeSent.get();
                    }
                    forgoMailMethod = true;
                } else {
                    checkCode(user, command);
                }
            }
        }
        userCommandPort.updateProfile(command);
        if (forgoMailMethod) {
            addressChangeCode.forgoMailMethod(user.id());
        }
        if (addressChanges) {
            // A reset link already sent went to the old address: whoever still reads it must not keep a
            // way in once the account has moved on.
            accountRecovery.forgetResetLinks(user.id());
        }
        if (addressChanges && user.email() != null) {
            profileMail.emailChanged(user.username(), user.email(), command.email(), user.language());
        }
        return forgoMailMethod ? new ProfileUpdate.SavedMailMethodRemoved() : new ProfileUpdate.Saved();
    }

    private void checkCurrentPassword(User user, UpdateProfileCommand command) {
        if (command.currentPassword() == null || command.currentPassword().isBlank()) {
            throw new IdentityException.CurrentPasswordRequired();
        }
        if (!passwordCheck.matches(user.id(), command.currentPassword())) {
            throw new IdentityException.InvalidCurrentPassword();
        }
    }

    /** Before any code leaves: it would land in someone else's mailbox. */
    private void refuseTakenAddress(User user, UpdateProfileCommand command) {
        boolean taken = userRepository.findByEmail(new EmailAddress(command.email()))
            .filter(other -> !other.id().equals(user.id()))
            .isPresent();
        if (taken) {
            throw new IdentityException.EmailAlreadyExists();
        }
    }

    /** @return the code sent, nothing saved yet — or empty while mail is off, when nothing can prove the address */
    private Optional<ProfileUpdate> sendCode(User user, UpdateProfileCommand command) {
        return switch (addressChangeCode.send(user.id(), command.email())) {
            case EmailCodeDelivery.Sent sent ->
                Optional.of(new ProfileUpdate.EmailCodeSent(command.email(), sent.resendAfterSeconds()));
            case EmailCodeDelivery.TooSoon tooSoon -> throw new IdentityException.EmailCodeResendTooSoon(tooSoon.retryAfterSeconds());
            case EmailCodeDelivery.LimitReached limit -> throw new IdentityException.EmailCodeSendLimitReached(limit.retryAfterSeconds());
            case EmailCodeDelivery.Unavailable unavailable -> Optional.empty();
        };
    }

    /** Checked even if mail went off since it was sent: the code still proves the address. */
    private void checkCode(User user, UpdateProfileCommand command) {
        switch (addressChangeCode.check(user.id(), command.email(), command.emailCode())) {
            case VALID -> { }
            case INVALID -> throw new IdentityException.EmailCodeInvalid();
            case EXPIRED -> throw new IdentityException.EmailCodeExpired();
            // The fifth wrong code took the code with it: even the right one is refused from now on.
            case SPENT -> throw new IdentityException.EmailCodeSpent();
        }
    }
}
