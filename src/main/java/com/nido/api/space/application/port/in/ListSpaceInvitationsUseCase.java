package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListSpaceInvitationsUseCase {
    List<SpaceInvitationView> list(SpaceMembership caller);
}
