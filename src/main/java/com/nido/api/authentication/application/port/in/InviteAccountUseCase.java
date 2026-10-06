package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.application.dto.InvitationDelivery;

import java.util.Optional;
import java.util.UUID;

public interface InviteAccountUseCase {

    /**
     * A new invitation link for the account, replacing any previous one: mailed when mail is on, otherwise
     * handed back to be shown once.
     *
     * @param inviterName the administrator named in the mail
     * @throws com.nido.api.authentication.domain.model.AuthenticationException.UserNotFound for an unknown account
     */
    InvitationDelivery invite(UUID userId, String inviterName);

    /**
     * A new link for an account still invited, replacing its previous one — looked at under lock, so an
     * acceptance committing meanwhile is seen. Empty once the account has chosen its password.
     *
     * @param inviterName the administrator named in the mail
     */
    Optional<InvitationDelivery> inviteAgain(UUID userId, String inviterName);
}
