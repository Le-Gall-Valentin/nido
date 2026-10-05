package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.InvitationDelivery;
import com.nido.api.identity.domain.model.InvitationState;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** The invitations of accounts that have not chosen their password yet — authentication keeps them. */
public interface AccountInvitationPort {

    /** A new invitation link for the account, replacing any previous one: mailed when mail is on, otherwise handed back. */
    InvitationDelivery invite(UUID userId, String inviterName);

    /** Whether the account was invited and has not chosen its password yet, its invitation pending or expired. */
    boolean isInvited(UUID userId);

    /** The invitations among these accounts, by account; an account that joined is absent. */
    Map<UUID, InvitationState> invitationsAmong(Collection<UUID> userIds);
}
