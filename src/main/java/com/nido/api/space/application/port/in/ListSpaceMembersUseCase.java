package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceMemberView;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListSpaceMembersUseCase {
    List<SpaceMemberView> list(SpaceMembership caller);
}
