package com.nido.api.identity.application.service;

import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.domain.port.out.AdminAccountMailPort;
import com.nido.api.identity.domain.port.out.AdminActivityNotificationPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Who hears about what an administrator does to an account — every rule in one place.
 *
 * <ul>
 *   <li>The account's holder, by mail nobody can switch off — unless they were only invited and never joined:
 *   then only the cancellation of their invitation is worth telling. A creation is told by the invitation
 *   itself.</li>
 *   <li>Every active super-administrator, by notification — only for what an ADMIN does. A role change has
 *   none: an ADMIN cannot change one.</li>
 * </ul>
 *
 * <p>Called inside the gesture's transaction, once its change is written. The holder's mail is queued in that
 * transaction and the notifications leave after its commit: a gesture that rolls back tells nobody. An
 * administrator who cannot be named tells nobody either, with a warning — never an exception, which would
 * undo the gesture.
 */
@ApplicationService
public class AdminGestureNotifier {

    private static final Logger log = LoggerFactory.getLogger(AdminGestureNotifier.class);

    private final UserRepository users;
    private final AccountInvitationPort invitations;
    private final AdminAccountMailPort mails;
    private final AdminActivityNotificationPort activity;

    public AdminGestureNotifier(UserRepository users, AccountInvitationPort invitations,
                                AdminAccountMailPort mails, AdminActivityNotificationPort activity) {
        this.users = users;
        this.invitations = invitations;
        this.mails = mails;
        this.activity = activity;
    }

    /** @param actorName already read by the creation, which refuses an administrator it cannot name */
    public void accountCreated(User account, String actorName, Role actorRole) {
        tellSuperAdministrators(actorRole, readers -> activity.accountCreated(readers, actorName, account.username()));
    }

    public void roleChanged(User account, Role newRole, UUID actorId) {
        nameOf(actorId).ifPresent(actor -> {
            if (!invitations.isInvited(account.id())) {
                mails.roleChanged(account, actor, newRole);
            }
        });
    }

    /** Called only when a method was actually removed — an invited account never has one. */
    public void twoFactorReset(User account, Set<TwoFactorMethod> removed, Set<TwoFactorMethod> kept,
                               UUID actorId, Role actorRole) {
        nameOf(actorId).ifPresent(actor -> {
            mails.twoFactorReset(account, actor, removed, kept);
            tellSuperAdministrators(actorRole, readers -> activity.twoFactorReset(readers, actor, account.username(), removed));
        });
    }

    public void deactivated(User account, UUID actorId, Role actorRole) {
        nameOf(actorId).ifPresent(actor -> {
            if (!invitations.isInvited(account.id())) {
                mails.deactivated(account, actor);
            }
            tellSuperAdministrators(actorRole, readers -> activity.accountDeactivated(readers, actor, account.username()));
        });
    }

    public void reactivated(User account, UUID actorId, Role actorRole) {
        nameOf(actorId).ifPresent(actor -> {
            if (!invitations.isInvited(account.id())) {
                mails.reactivated(account, actor);
            }
            tellSuperAdministrators(actorRole, readers -> activity.accountReactivated(readers, actor, account.username()));
        });
    }

    /**
     * @param account    as loaded before the anonymisation: its address and name are still in it
     * @param wasInvited read before the deletion, which takes the invitation with the rest
     */
    public void deleted(User account, boolean wasInvited, UUID actorId, Role actorRole) {
        nameOf(actorId).ifPresent(actor -> {
            if (wasInvited) {
                mails.invitationCancelled(account, actor);
            } else {
                mails.deleted(account, actor);
            }
            tellSuperAdministrators(actorRole, readers -> activity.accountDeleted(readers, actor, account.username(), wasInvited));
        });
    }

    private void tellSuperAdministrators(Role actorRole, Consumer<List<User>> tell) {
        if (actorRole != Role.ADMIN) {
            return;
        }
        List<User> readers = users.findActiveByRole(Role.SUPER_ADMIN);
        if (!readers.isEmpty()) {
            tell.accept(readers);
        }
    }

    private Optional<String> nameOf(UUID actorId) {
        Optional<String> name = users.findById(actorId).map(User::username);
        if (name.isEmpty()) {
            log.warn("Administrator {} cannot be named: nobody is told of their gesture", actorId);
        }
        return name;
    }
}
