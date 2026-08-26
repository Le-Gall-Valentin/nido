package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.InviteMemberCommand;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;

public interface InviteMemberUseCase {
    SpaceInvitationView invite(InviteMemberCommand command, SpaceMembership caller);
}
