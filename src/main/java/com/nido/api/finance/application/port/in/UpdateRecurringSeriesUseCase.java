package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateRecurringSeriesUseCase {
    RecurringTransactionSeries update(UpdateRecurringSeriesCommand command, SpaceMembership caller);
}
