package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.RevokeInvitationUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class RevokeInvitationHandler implements RevokeInvitationUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevokeInvitationHandler.class);

    private final SpaceInvitationPort spaceInvitationPort;

    public RevokeInvitationHandler(SpaceInvitationPort spaceInvitationPort) {
        this.spaceInvitationPort = spaceInvitationPort;
    }

    @Override
    @Transactional
    public void revoke(UUID spaceId, UUID invitationId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanManageSpace();
        SpaceInvitation invitation = spaceInvitationPort.findById(invitationId)
            .orElseThrow(SpaceException.InvitationNotFound::new);
        if (!invitation.spaceId().equals(spaceId)) {
            // Une invitation d'un autre contexte est traitée comme inexistante : connaître
            // son identifiant ne doit rien permettre ni rien révéler.
            throw new SpaceException.InvitationNotFound();
        }
        invitation.ensurePending();
        spaceInvitationPort.revoke(invitationId);
        log.info("Invitation {} revoked in space {} by user {}", invitationId, spaceId, caller.userId());
    }
}
