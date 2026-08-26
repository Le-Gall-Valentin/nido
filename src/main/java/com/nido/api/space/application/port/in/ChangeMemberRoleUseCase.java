package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.ChangeMemberRoleCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface ChangeMemberRoleUseCase {
    void change(ChangeMemberRoleCommand command, SpaceMembership caller);
}
