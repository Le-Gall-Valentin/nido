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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Who hears about what happens in a space — every rule of the space notifications, in one place. The
 * handlers call it inside their transaction, after their change is written (before it, for a deletion,
 * which takes the memberships with it), and pass ids only: the space, its members and their names are
 * read here, the names in one query.
 *
 * <p>The author of a change is never told about it. A reader who can no longer be named (an anonymised
 * account) is left out; an event whose author or subject can no longer be named tells nobody, with a
 * warning — never an exception, which would undo the change it is about.
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
        Optional<Space> space = spaces.findById(invitation.spaceId());
        if (space.isEmpty()) {
            return;
        }
        String inviterName = memberNames.byId(List.of(inviterId)).get(inviterId);
        if (inviterName == null) {
            unnamed("invitation", invitation.spaceId());
            return;
        }
        notifications.invitationIssued(invitation, inviteeName, inviterName, space.get().name());
    }

    public void memberJoined(UUID spaceId, UUID memberId) {
        Optional<Space> space = spaces.findById(spaceId);
        List<UUID> readers = membersExcept(spaceId, memberId);
        if (space.isEmpty() || readers.isEmpty()) {
            return;
        }
        Map<UUID, String> names = namesOf(readers, memberId);
        if (!names.containsKey(memberId)) {
            unnamed("member joined", spaceId);
            return;
        }
        notifications.memberJoined(spaceId, space.get().name(), names.get(memberId), addressees(readers, names));
    }

    public void memberLeft(UUID spaceId, UUID memberId) {
        Optional<Space> space = spaces.findById(spaceId);
        List<UUID> readers = membersExcept(spaceId, memberId);
        if (space.isEmpty() || readers.isEmpty()) {
            return;
        }
        Map<UUID, String> names = namesOf(readers, memberId);
        if (!names.containsKey(memberId)) {
            unnamed("member left", spaceId);
            return;
        }
        notifications.memberLeft(spaceId, space.get().name(), names.get(memberId), addressees(readers, names));
    }

    public void memberRemoved(UUID spaceId, UUID actorId, UUID removedId) {
        Optional<Space> space = spaces.findById(spaceId);
        if (space.isEmpty()) {
            return;
        }
        List<UUID> others = membersExcept(spaceId, actorId, removedId);
        Map<UUID, String> names = namesOf(others, actorId, removedId);
        if (!names.containsKey(actorId) || !names.containsKey(removedId)) {
            unnamed("member removed", spaceId);
            return;
        }
        String spaceName = space.get().name();
        notifications.removedFromSpace(spaceName, names.get(actorId), new Addressee(removedId, names.get(removedId)));
        if (!others.isEmpty()) {
            notifications.memberRemoved(spaceId, spaceName, names.get(actorId), names.get(removedId), addressees(others, names));
        }
    }

    /** Before the deletion: it takes the memberships with it. */
    public void spaceDeleted(UUID spaceId, UUID actorId) {
        Optional<Space> space = spaces.findById(spaceId);
        List<UUID> readers = membersExcept(spaceId, actorId);
        if (space.isEmpty() || readers.isEmpty()) {
            return;
        }
        Map<UUID, String> names = namesOf(readers, actorId);
        if (!names.containsKey(actorId)) {
            unnamed("space deleted", spaceId);
            return;
        }
        notifications.spaceDeleted(space.get().name(), names.get(actorId), addressees(readers, names));
    }

    public void roleChanged(UUID spaceId, UUID actorId, UUID memberId, SpaceRole previousRole, SpaceRole newRole) {
        Optional<Space> space = spaces.findById(spaceId);
        if (space.isEmpty()) {
            return;
        }
        Map<UUID, String> names = namesOf(List.of(), actorId, memberId);
        if (!names.containsKey(actorId) || !names.containsKey(memberId)) {
            unnamed("role changed", spaceId);
            return;
        }
        notifications.roleChanged(spaceId, space.get().name(), names.get(actorId),
            new Addressee(memberId, names.get(memberId)), previousRole, newRole);
    }

    public void ownershipTransferred(UUID spaceId, UUID previousOwnerId, UUID newOwnerId) {
        Optional<Space> space = spaces.findById(spaceId);
        if (space.isEmpty()) {
            return;
        }
        List<UUID> others = membersExcept(spaceId, previousOwnerId, newOwnerId);
        Map<UUID, String> names = namesOf(others, previousOwnerId, newOwnerId);
        if (!names.containsKey(previousOwnerId) || !names.containsKey(newOwnerId)) {
            unnamed("ownership transferred", spaceId);
            return;
        }
        newOwner(spaceId, space.get().name(), names.get(previousOwnerId), new Addressee(newOwnerId, names.get(newOwnerId)),
            addressees(others, names));
    }

    /** The owner's account was deleted and the ownership passed on: there is no author to name. */
    public void ownershipInherited(UUID spaceId, UUID newOwnerId) {
        Optional<Space> space = spaces.findById(spaceId);
        if (space.isEmpty()) {
            return;
        }
        List<UUID> others = membersExcept(spaceId, newOwnerId);
        Map<UUID, String> names = namesOf(others, newOwnerId);
        if (!names.containsKey(newOwnerId)) {
            unnamed("ownership inherited", spaceId);
            return;
        }
        newOwner(spaceId, space.get().name(), null, new Addressee(newOwnerId, names.get(newOwnerId)), addressees(others, names));
    }

    private void newOwner(UUID spaceId, String spaceName, String actorName, Addressee newOwner, List<Addressee> others) {
        notifications.ownershipReceived(spaceId, spaceName, actorName, newOwner);
        if (!others.isEmpty()) {
            notifications.ownerChanged(spaceId, spaceName, actorName, newOwner.username(), others);
        }
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

    private static List<Addressee> addressees(List<UUID> readers, Map<UUID, String> names) {
        return readers.stream()
            .filter(names::containsKey)
            .map(userId -> new Addressee(userId, names.get(userId)))
            .toList();
    }

    private static void unnamed(String event, UUID spaceId) {
        log.warn("Space {}: {} — an account it names can no longer be named, nobody is notified", spaceId, event);
    }
}
