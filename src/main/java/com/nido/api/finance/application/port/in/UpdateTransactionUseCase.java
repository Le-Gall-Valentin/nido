package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateTransactionUseCase {
    Transaction update(UpdateTransactionCommand command, SpaceMembership caller);
}
