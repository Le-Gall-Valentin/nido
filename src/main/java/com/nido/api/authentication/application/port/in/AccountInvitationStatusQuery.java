package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.application.dto.InvitationStatus;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Which accounts were invited and have not chosen their password yet. */
public interface AccountInvitationStatusQuery {

    /** Whether the account has an invitation, pending or expired — that is, no password yet. */
    boolean isInvited(UUID userId);

    /** The invitations among these accounts, by account; an account that joined is absent. */
    Map<UUID, InvitationStatus> statusAmong(Collection<UUID> userIds);
}
