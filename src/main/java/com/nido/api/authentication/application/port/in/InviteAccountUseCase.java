package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.application.dto.InvitationDelivery;

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
}
