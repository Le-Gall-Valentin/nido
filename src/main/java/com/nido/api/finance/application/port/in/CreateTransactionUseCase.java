package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.space.domain.model.SpaceMembership;

public interface CreateTransactionUseCase {
    Transaction create(CreateTransactionCommand command, SpaceMembership caller);
}
