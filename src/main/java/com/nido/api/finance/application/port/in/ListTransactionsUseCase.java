package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.YearMonth;
import java.util.List;

public interface ListTransactionsUseCase {
    List<Transaction> list(YearMonth month, SpaceMembership caller);
}
