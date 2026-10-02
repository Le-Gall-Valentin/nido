package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.service.MemberNames;
import com.nido.api.space.application.port.in.InviteMemberUseCase;
import com.nido.api.space.domain.model.InviteMemberCommand;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.InvitationCodeGeneratorPort;
import com.nido.api.space.domain.port.out.InvitationNotificationPort;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationService
public class InviteMemberHandler implements InviteMemberUseCase {

    private static final Logger log = LoggerFactory.getLogger(InviteMemberHandler.class);

    private final SpaceInvitationPort spaceInvitationPort;
    private final InvitationCodeGeneratorPort invitationCodeGeneratorPort;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipPort spaceMembershipPort;
    private final MemberProfilePort memberProfilePort;
    private final InvitationNotificationPort invitationNotificationPort;
    private final MemberNames memberNames;

    public InviteMemberHandler(SpaceInvitationPort spaceInvitationPort,
                               InvitationCodeGeneratorPort invitationCodeGeneratorPort,
                               SpaceRepository spaceRepository,
                               SpaceMembershipPort spaceMembershipPort,
                               MemberProfilePort memberProfilePort,
                               InvitationNotificationPort invitationNotificationPort,
                               MemberNames memberNames) {
        this.spaceInvitationPort = spaceInvitationPort;
        this.invitationCodeGeneratorPort = invitationCodeGeneratorPort;
        this.spaceRepository = spaceRepository;
        this.spaceMembershipPort = spaceMembershipPort;
        this.memberProfilePort = memberProfilePort;
        this.invitationNotificationPort = invitationNotificationPort;
        this.memberNames = memberNames;
    }

    @Override
    @Transactional
    public SpaceInvitationView invite(InviteMemberCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanManageSpace();
        Space space = spaceRepository.findById(command.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        space.ensureShared();
        MemberProfile invitee = memberProfilePort.findByIdentifier(command.identifier())
            .orElseThrow(SpaceException.NoAccountForIdentifier::new);
        if (spaceMembershipPort.find(command.spaceId(), invitee.userId()).isPresent()) {
            throw new SpaceException.AlreadyMember();
        }
        SpaceInvitation invitation = spaceInvitationPort.create(
            command.spaceId(), invitee.userId(), command.role(),
            invitationCodeGeneratorPort.generate(),
            Instant.now().plus(InviteMemberCommand.VALIDITY),
            caller.userId());
        log.info("Invitation {} issued for space {} by user {}",
            invitation.id(), command.spaceId(), caller.userId());
        tellTheInvitee(invitation, invitee, space, caller.userId());
        return SpaceInvitationView.of(invitation, invitee.username());
    }

    /**
     * In this transaction, once every check has passed: a refused or rolled-back invitation tells nobody.
     * An account that can sign in always has a name; one that lost it was erased while it was inviting.
     */
    private void tellTheInvitee(SpaceInvitation invitation, MemberProfile invitee, Space space, UUID inviterId) {
        String inviterName = memberNames.byId(List.of(inviterId)).get(inviterId);
        if (inviterName == null) {
            log.warn("Invitation {} was issued by user {}, who can no longer be named: the invitee is not notified",
                invitation.id(), inviterId);
            return;
        }
        invitationNotificationPort.invitationIssued(invitation, invitee.username(), inviterName, space.name());
    }
}
