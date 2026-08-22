package com.nido.api.space.infrastructure.persistence.adapter;

import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.infrastructure.persistence.entity.SpaceInvitationEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceInvitationJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SpaceInvitationRepositoryAdapter implements SpaceInvitationPort {

    private final SpaceInvitationJpaRepository invitations;

    public SpaceInvitationRepositoryAdapter(SpaceInvitationJpaRepository invitations) {
        this.invitations = invitations;
    }

    @Override
    public SpaceInvitation create(UUID spaceId, String email, SpaceRole role, String code,
                                   Instant expiresAt, UUID createdBy) {
        try {
            SpaceInvitationEntity e = new SpaceInvitationEntity();
            e.setSpaceId(spaceId);
            e.setEmail(email);
            e.setRole(role);
            e.setCode(code);
            e.setStatus(InvitationStatus.PENDING);
            e.setExpiresAt(expiresAt);
            e.setCreatedBy(createdBy);
            return toDomain(invitations.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw resolveConstraintViolation(ex);
        }
    }

    @Override
    public Optional<SpaceInvitation> findById(UUID invitationId) {
        return invitations.findById(invitationId).map(SpaceInvitationRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<SpaceInvitation> findByCode(String code) {
        return invitations.findByCode(code).map(SpaceInvitationRepositoryAdapter::toDomain);
    }

    @Override
    public List<SpaceInvitation> findBySpace(UUID spaceId) {
        return invitations.findBySpaceIdOrderByCreatedAtDesc(spaceId).stream()
            .map(SpaceInvitationRepositoryAdapter::toDomain)
            .toList();
    }

    @Override
    public List<SpaceInvitation> findPendingForEmail(String email, Instant now) {
        return invitations.findPendingForEmail(email, now).stream()
            .map(SpaceInvitationRepositoryAdapter::toDomain)
            .toList();
    }

    @Override
    public boolean claim(UUID invitationId, Instant acceptedAt) {
        return invitations.claim(invitationId, acceptedAt) == 1;
    }

    @Override
    public void revoke(UUID invitationId) {
        if (invitations.revoke(invitationId) == 0) {
            throw new SpaceException.InvitationNotPending();
        }
    }

    @Override
    public int deleteAllForEmail(String email) {
        return invitations.deleteAllForEmail(email);
    }

    private SpaceException resolveConstraintViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String c = cve.getConstraintName();
            if (c != null) {
                if (c.contains("uq_space_invitations_pending")) return new SpaceException.InvitationAlreadyPending();
            }
        }
        return new SpaceException.DataIntegrityError();
    }

    private static SpaceInvitation toDomain(SpaceInvitationEntity e) {
        return new SpaceInvitation(e.getId(), e.getSpaceId(), e.getEmail(), e.getRole(), e.getCode(),
            e.getStatus(), e.getExpiresAt(), e.getCreatedBy(), e.getAcceptedAt(), e.getCreatedAt());
    }
}
