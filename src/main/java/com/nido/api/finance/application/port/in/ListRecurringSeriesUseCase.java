package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListRecurringSeriesUseCase {
    List<RecurringTransactionSeries> list(SpaceMembership caller);
}
