package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface RevokeInvitationUseCase {
    void revoke(UUID spaceId, UUID invitationId, SpaceMembership caller);
}
