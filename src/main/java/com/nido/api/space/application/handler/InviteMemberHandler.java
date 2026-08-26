package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.InviteMemberUseCase;
import com.nido.api.space.domain.model.InviteMemberCommand;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.InvitationCodeGeneratorPort;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@ApplicationService
public class InviteMemberHandler implements InviteMemberUseCase {

    private static final Logger log = LoggerFactory.getLogger(InviteMemberHandler.class);

    private final SpaceInvitationPort spaceInvitationPort;
    private final InvitationCodeGeneratorPort invitationCodeGeneratorPort;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipPort spaceMembershipPort;
    private final MemberProfilePort memberProfilePort;

    public InviteMemberHandler(SpaceInvitationPort spaceInvitationPort,
                               InvitationCodeGeneratorPort invitationCodeGeneratorPort,
                               SpaceRepository spaceRepository,
                               SpaceMembershipPort spaceMembershipPort,
                               MemberProfilePort memberProfilePort) {
        this.spaceInvitationPort = spaceInvitationPort;
        this.invitationCodeGeneratorPort = invitationCodeGeneratorPort;
        this.spaceRepository = spaceRepository;
        this.spaceMembershipPort = spaceMembershipPort;
        this.memberProfilePort = memberProfilePort;
    }

    @Override
    @Transactional
    public SpaceInvitationView invite(InviteMemberCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanManageSpace();
        Space space = spaceRepository.findById(command.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        space.ensureShared();
        MemberProfile invitee = memberProfilePort.findByEmail(command.email())
            .orElseThrow(SpaceException.NoAccountForEmail::new);
        if (spaceMembershipPort.find(command.spaceId(), invitee.userId()).isPresent()) {
            throw new SpaceException.AlreadyMember();
        }
        SpaceInvitation invitation = spaceInvitationPort.create(
            command.spaceId(), command.email(), command.role(),
            invitationCodeGeneratorPort.generate(),
            Instant.now().plus(InviteMemberCommand.VALIDITY),
            caller.userId());
        log.info("Invitation {} issued for space {} by user {}",
            invitation.id(), command.spaceId(), caller.userId());
        return toView(invitation);
    }

    private static SpaceInvitationView toView(SpaceInvitation invitation) {
        return new SpaceInvitationView(invitation.id(), invitation.email(), invitation.role(),
            invitation.code(), invitation.status(), invitation.expiresAt(), invitation.createdAt());
    }
}
