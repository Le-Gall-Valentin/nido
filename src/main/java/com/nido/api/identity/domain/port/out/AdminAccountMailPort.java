package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.User;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Set;

/**
 * What an account's holder is told when an administrator acts on their account. Mails, not notifications:
 * nobody can switch them off. Accepted and dropped when mail is off.
 */
public interface AdminAccountMailPort {

    void roleChanged(User account, String actorName, Role newRole);

    /**
     * @param removed what the administrator took away — never empty
     * @param kept    what still protects the account
     */
    void twoFactorReset(User account, String actorName, Set<TwoFactorMethod> removed, Set<TwoFactorMethod> kept);

    void deactivated(User account, String actorName);

    void reactivated(User account, String actorName);

    /** @param account read before the anonymisation, which wipes its address and name */
    void deleted(User account, String actorName);

    /** The account was only invited: its invitation, not an account it never used, is what ends. */
    void invitationCancelled(User account, String actorName);
}
