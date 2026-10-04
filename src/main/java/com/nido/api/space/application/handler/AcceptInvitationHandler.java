package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.service.SpaceNotifier;
import com.nido.api.space.application.port.in.AcceptInvitationUseCase;
import com.nido.api.space.domain.model.AcceptInvitationCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationService
public class AcceptInvitationHandler implements AcceptInvitationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AcceptInvitationHandler.class);

    private final SpaceInvitationPort spaceInvitationPort;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipPort spaceMembershipPort;
    private final SpaceNotifier spaceNotifier;

    public AcceptInvitationHandler(SpaceInvitationPort spaceInvitationPort, SpaceRepository spaceRepository,
                                   SpaceMembershipPort spaceMembershipPort,
                                   SpaceNotifier spaceNotifier) {
        this.spaceInvitationPort = spaceInvitationPort;
        this.spaceRepository = spaceRepository;
        this.spaceMembershipPort = spaceMembershipPort;
        this.spaceNotifier = spaceNotifier;
    }

    @Override
    @Transactional
    public UUID accept(AcceptInvitationCommand command, UUID userId) {
        SpaceInvitation invitation = spaceInvitationPort.findByCode(command.code())
            .orElseThrow(SpaceException.InvitationNotFound::new);
        return acceptInvitation(invitation, userId);
    }

    @Override
    @Transactional
    public UUID acceptById(UUID invitationId, UUID userId) {
        SpaceInvitation invitation = spaceInvitationPort.findById(invitationId)
            .orElseThrow(SpaceException.InvitationNotFound::new);
        return acceptInvitation(invitation, userId);
    }

    // Chemin partagé par accept() et acceptById() : seule la façon de retrouver l'invitation
    // diffère entre les deux entrées, tout le reste — l'enchaînement des garde-fous et des
    // écritures — doit rester rigoureusement identique.
    private UUID acceptInvitation(SpaceInvitation invitation, UUID userId) {
        Instant now = Instant.now();
        // L'ordre compte : le destinataire d'abord. Un appelant qui soumet des codes ou des
        // identifiants au hasard ne doit pas pouvoir distinguer « inexistant » de « destiné
        // à autrui », et les deux rendent le même 404 (cf. SpaceExceptionHandler).
        invitation.ensureAddressedTo(userId);
        invitation.ensurePending();
        invitation.ensureNotExpired(now);
        Space space = spaceRepository.findById(invitation.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        if (spaceMembershipPort.find(space.id(), userId).isPresent()) {
            throw new SpaceException.AlreadyMember();
        }
        // Réclamer avant de créer l'adhésion : la mise à jour conditionnelle est le point
        // de sérialisation, donc deux acceptations concurrentes n'en voient qu'une réussir.
        if (!spaceInvitationPort.claim(invitation.id(), now)) {
            throw new SpaceException.InvitationNotPending();
        }
        spaceMembershipPort.add(space.id(), userId, invitation.role());
        log.info("User {} joined space {} through invitation {}", userId, space.id(), invitation.id());
        spaceNotifier.memberJoined(space.id(), userId);
        return space.id();
    }
}
