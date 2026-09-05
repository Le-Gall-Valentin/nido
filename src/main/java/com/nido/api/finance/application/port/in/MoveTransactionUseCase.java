package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface MoveTransactionUseCase {
    Transaction move(UUID transactionId, UUID destinationSpaceId, SpaceMembership caller);
}
