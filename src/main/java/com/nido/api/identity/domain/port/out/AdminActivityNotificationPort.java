package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.User;

import java.util.List;

/**
 * What super-administrators hear of the gestures of administrators — notifications, each reader can switch
 * them off. One mail per reader, greeting them.
 */
public interface AdminActivityNotificationPort {

    void accountCreated(List<User> readers, String actorName, String accountName);

    void accountDeactivated(List<User> readers, String actorName, String accountName);

    void accountReactivated(List<User> readers, String actorName, String accountName);

    /**
     * @param accountName read before the anonymisation: the deleted account is named, as everywhere else
     * @param wasInvited  the account never joined: its invitation is what ends
     */
    void accountDeleted(List<User> readers, String actorName, String accountName, boolean wasInvited);

    void totpReset(List<User> readers, String actorName, String accountName);
}
