package com.nido.api.space.domain.port.out;

import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceInvitationPort {

    SpaceInvitation create(UUID spaceId, String email, SpaceRole role, String code,
                           Instant expiresAt, UUID createdBy);

    Optional<SpaceInvitation> findById(UUID invitationId);

    Optional<SpaceInvitation> findByCode(String code);

    List<SpaceInvitation> findBySpace(UUID spaceId);

    /** Invitations en attente et non expirées adressées à cette adresse, insensible à la casse. */
    List<SpaceInvitation> findPendingForEmail(String email, Instant now);

    /**
     * Passe l'invitation de PENDING à ACCEPTED de façon atomique.
     * Retourne false si elle ne l'était plus, ce qui règle la course entre deux acceptations
     * concurrentes sans verrou applicatif.
     */
    boolean claim(UUID invitationId, Instant acceptedAt);

    void revoke(UUID invitationId);

    /** Supprime toutes les invitations adressées à cet email, quel que soit leur statut. */
    int deleteAllForEmail(String email);
}
