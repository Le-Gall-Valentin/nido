package com.nido.api.space.application.service;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceNotificationPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Who hears about what happens in a space — every rule of the space notifications, in one place. The
 * handlers call it inside their transaction, after their change is written (before it, for a deletion,
 * which takes the memberships with it), and pass ids only: the space, its members and their names are
 * read here, the names in one query.
 *
 * <p>The author of a change is never told about it. A reader who can no longer be named (an anonymised
 * account) is left out; an event whose author or subject can no longer be named tells nobody, with a
 * warning naming the accounts by id — never an exception, which would undo the change it is about.
 */
@ApplicationService
public class SpaceNotifier {

    private static final Logger log = LoggerFactory.getLogger(SpaceNotifier.class);

    private final SpaceRepository spaces;
    private final SpaceMembershipPort memberships;
    private final MemberNames memberNames;
    private final SpaceNotificationPort notifications;

    public SpaceNotifier(SpaceRepository spaces, SpaceMembershipPort memberships, MemberNames memberNames,
                         SpaceNotificationPort notifications) {
        this.spaces = spaces;
        this.memberships = memberships;
        this.memberNames = memberNames;
        this.notifications = notifications;
    }

    public void invitationIssued(SpaceInvitation invitation, String inviteeName, UUID inviterId) {
        inSpace(invitation.spaceId(), spaceName -> {
            Map<UUID, String> names = namesOf(List.of(), inviterId);
            if (unnamed(names, "invitation " + invitation.id(), invitation.spaceId(), inviterId)) {
                return;
            }
            notifications.invitationIssued(invitation, inviteeName, names.get(inviterId), spaceName);
        });
    }

    public void memberJoined(UUID spaceId, UUID memberId) {
        announceMember(spaceId, memberId, "member joined", notifications::memberJoined);
    }

    public void memberLeft(UUID spaceId, UUID memberId) {
        announceMember(spaceId, memberId, "member left", notifications::memberLeft);
    }

    public void memberRemoved(UUID spaceId, UUID actorId, UUID removedId) {
        inSpace(spaceId, spaceName -> {
            List<UUID> others = membersExcept(spaceId, actorId, removedId);
            Map<UUID, String> names = namesOf(others, actorId, removedId);
            if (unnamed(names, "member removed", spaceId, actorId, removedId)) {
                return;
            }
            notifications.removedFromSpace(spaceName, names.get(actorId), new Addressee(removedId, names.get(removedId)));
            if (!others.isEmpty()) {
                notifications.memberRemoved(spaceId, spaceName, names.get(actorId), names.get(removedId),
                    addressees(others, names));
            }
        });
    }

    /** Before the deletion: it takes the memberships with it. */
    public void spaceDeleted(UUID spaceId, UUID actorId) {
        inSpace(spaceId, spaceName -> {
            List<UUID> readers = membersExcept(spaceId, actorId);
            if (readers.isEmpty()) {
                return;
            }
            Map<UUID, String> names = namesOf(readers, actorId);
            if (unnamed(names, "space deleted", spaceId, actorId)) {
                return;
            }
            notifications.spaceDeleted(spaceName, names.get(actorId), addressees(readers, names));
        });
    }

    public void roleChanged(UUID spaceId, UUID actorId, UUID memberId, SpaceRole previousRole, SpaceRole newRole) {
        inSpace(spaceId, spaceName -> {
            Map<UUID, String> names = namesOf(List.of(), actorId, memberId);
            if (unnamed(names, "role changed", spaceId, actorId, memberId)) {
                return;
            }
            notifications.roleChanged(spaceId, spaceName, names.get(actorId),
                new Addressee(memberId, names.get(memberId)), previousRole, newRole);
        });
    }

    public void ownershipTransferred(UUID spaceId, UUID previousOwnerId, UUID newOwnerId) {
        inSpace(spaceId, spaceName -> {
            List<UUID> others = membersExcept(spaceId, previousOwnerId, newOwnerId);
            Map<UUID, String> names = namesOf(others, previousOwnerId, newOwnerId);
            if (unnamed(names, "ownership transferred", spaceId, previousOwnerId, newOwnerId)) {
                return;
            }
            Addressee newOwner = new Addressee(newOwnerId, names.get(newOwnerId));
            notifications.ownershipReceived(spaceId, spaceName, names.get(previousOwnerId), newOwner);
            if (!others.isEmpty()) {
                notifications.ownerChanged(spaceId, spaceName, names.get(previousOwnerId), newOwner.username(),
                    addressees(others, names));
            }
        });
    }

    /** A member's account was deleted (an owner's goes through ownershipInherited): the others hear they left Nido. */
    public void memberLeftNido(UUID spaceId, UUID memberId, String memberName) {
        inSpace(spaceId, spaceName -> {
            List<UUID> readers = membersExcept(spaceId, memberId);
            if (readers.isEmpty()) {
                return;
            }
            Map<UUID, String> names = namesWithErased(readers, memberId, memberName);
            if (unnamed(names, "member left Nido", spaceId, memberId)) {
                return;
            }
            notifications.memberLeftNido(spaceId, spaceName, memberName, addressees(readers, names));
        });
    }

    /** The owner's account was deleted and {@code heirId} inherited the space: the heir and the others hear who left. */
    public void ownershipInherited(UUID spaceId, UUID formerOwnerId, String formerOwnerName, UUID heirId) {
        inSpace(spaceId, spaceName -> {
            List<UUID> others = membersExcept(spaceId, heirId, formerOwnerId);
            Map<UUID, String> names = namesWithErased(others, formerOwnerId, formerOwnerName);
            names.putAll(namesOf(List.of(), heirId));
            if (unnamed(names, "ownership inherited", spaceId, formerOwnerId, heirId)) {
                return;
            }
            Addressee heir = new Addressee(heirId, names.get(heirId));
            notifications.ownershipInherited(spaceId, spaceName, formerOwnerName, heir);
            List<Addressee> told = addressees(others, names);
            if (!told.isEmpty()) {
                notifications.ownerSucceeded(spaceId, spaceName, formerOwnerName, heir.username(), told);
            }
        });
    }

    /** A member's arrival or departure, told to everyone else in the space. */
    private void announceMember(UUID spaceId, UUID memberId, String event, MemberAnnouncement announcement) {
        inSpace(spaceId, spaceName -> {
            List<UUID> readers = membersExcept(spaceId, memberId);
            if (readers.isEmpty()) {
                return;
            }
            Map<UUID, String> names = namesOf(readers, memberId);
            if (unnamed(names, event, spaceId, memberId)) {
                return;
            }
            announcement.tell(spaceId, spaceName, names.get(memberId), addressees(readers, names));
        });
    }

    /** Hands the space's name to what is told about it; a space already gone tells nobody. */
    private void inSpace(UUID spaceId, Consumer<String> told) {
        spaces.findById(spaceId).map(Space::name).ifPresent(told);
    }

    private List<UUID> membersExcept(UUID spaceId, UUID... excluded) {
        Set<UUID> skipped = Set.copyOf(Arrays.asList(excluded));
        return memberships.findMemberships(spaceId).stream()
            .map(SpaceMembership::userId)
            .filter(userId -> !skipped.contains(userId))
            .toList();
    }

    private Map<UUID, String> namesOf(List<UUID> readers, UUID... mentioned) {
        List<UUID> ids = new ArrayList<>(readers);
        ids.addAll(Arrays.asList(mentioned));
        return memberNames.byId(ids);
    }

    /**
     * The readers' names, and the name of an account being erased — read by the caller before the
     * anonymisation, since it can no longer be looked up. A null name stays absent: the event is then unnamed.
     */
    private Map<UUID, String> namesWithErased(List<UUID> readers, UUID erasedId, String erasedName) {
        Map<UUID, String> names = new HashMap<>(namesOf(readers));
        if (erasedName != null) {
            names.put(erasedId, erasedName);
        }
        return names;
    }

    private static List<Addressee> addressees(List<UUID> readers, Map<UUID, String> names) {
        return readers.stream()
            .filter(names::containsKey)
            .map(userId -> new Addressee(userId, names.get(userId)))
            .toList();
    }

    /** True, with a warning naming them by id, when an account the event must name can no longer be named. */
    private static boolean unnamed(Map<UUID, String> names, String event, UUID spaceId, UUID... mentioned) {
        List<UUID> missing = Arrays.stream(mentioned).filter(id -> !names.containsKey(id)).toList();
        if (missing.isEmpty()) {
            return false;
        }
        log.warn("Space {}: {} — account(s) {} can no longer be named, nobody is notified", spaceId, event, missing);
        return true;
    }

    @FunctionalInterface
    private interface MemberAnnouncement {
        void tell(UUID spaceId, String spaceName, String memberName, List<Addressee> recipients);
    }
}
