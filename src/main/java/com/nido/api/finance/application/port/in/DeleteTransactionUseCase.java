package com.nido.api.finance.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteTransactionUseCase {
    void delete(UUID transactionId, UUID spaceId, SpaceMembership caller);
}
