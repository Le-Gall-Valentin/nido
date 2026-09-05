package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.space.domain.model.SpaceMembership;

public interface CreateRecurringSeriesUseCase {
    RecurringTransactionSeries create(CreateRecurringSeriesCommand command, SpaceMembership caller);
}
