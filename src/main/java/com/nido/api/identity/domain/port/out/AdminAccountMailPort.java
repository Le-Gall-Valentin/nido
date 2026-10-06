package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.User;
import com.nido.api.shared.model.Role;

/**
 * What an account's holder is told when an administrator acts on their account. Mails, not notifications:
 * nobody can switch them off. Accepted and dropped when mail is off.
 */
public interface AdminAccountMailPort {

    void roleChanged(User account, String actorName, Role newRole);

    void totpReset(User account, String actorName);

    void deactivated(User account, String actorName);

    void reactivated(User account, String actorName);

    /** @param account read before the anonymisation, which wipes its address and name */
    void deleted(User account, String actorName);

    /** The account was only invited: its invitation, not an account it never used, is what ends. */
    void invitationCancelled(User account, String actorName);
}
