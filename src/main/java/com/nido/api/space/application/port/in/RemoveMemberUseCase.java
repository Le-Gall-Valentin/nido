package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.RemoveMemberCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface RemoveMemberUseCase {
    void remove(RemoveMemberCommand command, SpaceMembership caller);
}
