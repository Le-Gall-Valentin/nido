package com.nido.api.identity.infrastructure.mail;

import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AdminAccountMailPort;
import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.mail.application.port.in.SendMailUseCase;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.MailRequest;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.shared.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * The mails an account's holder receives when an administrator acts on their account, in the account's
 * language — otherwise in the administrator's, the language of the request.
 */
@Component
public class AdminAccountMailAdapter implements AdminAccountMailPort {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountMailAdapter.class);

    /** An alert that outlives its moment is worse than none: a queue kept while mail was off must not deliver it weeks late. */
    private static final Duration ALERT_VALIDITY = Duration.ofHours(24);
    private static final AppPath LOGIN = new AppPath("/login");
    private static final AppPath HOME = new AppPath("/");
    private static final AppPath SECURITY = new AppPath("/account/security");

    private final SendMailUseCase sendMail;
    private final Clock clock;

    public AdminAccountMailAdapter(SendMailUseCase sendMail, Clock clock) {
        this.sendMail = sendMail;
        this.clock = clock;
    }

    @Override
    public void roleChanged(User account, String actorName, Role newRole) {
        send(account, new RoleChangedMail(account.username(), actorName, newRole == Role.ADMIN, HOME));
    }

    @Override
    public void totpReset(User account, String actorName) {
        send(account, new TotpResetMail(account.username(), actorName, SECURITY));
    }

    @Override
    public void deactivated(User account, String actorName) {
        send(account, new AccountDeactivatedMail(account.username(), actorName));
    }

    @Override
    public void reactivated(User account, String actorName) {
        send(account, new AccountReactivatedMail(account.username(), actorName, LOGIN));
    }

    @Override
    public void deleted(User account, String actorName) {
        send(account, new AccountDeletedMail(account.username(), actorName));
    }

    @Override
    public void invitationCancelled(User account, String actorName) {
        send(account, new InvitationCancelledMail(account.username(), actorName));
    }

    private void send(User account, MailContent content) {
        if (account.email() == null || account.email().isBlank()) {
            log.warn("Account {} has no address: {} not sent", account.id(), content.template());
            return;
        }
        sendMail.send(new MailRequest(new Recipient(account.email(), account.username()),
            MailLanguage.resolve(account.language()), content, clock.instant().plus(ALERT_VALIDITY)));
    }
}
