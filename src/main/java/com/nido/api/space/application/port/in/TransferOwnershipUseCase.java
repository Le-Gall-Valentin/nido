package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.TransferOwnershipCommand;

public interface TransferOwnershipUseCase {
    void transfer(TransferOwnershipCommand command, SpaceMembership caller);
}
