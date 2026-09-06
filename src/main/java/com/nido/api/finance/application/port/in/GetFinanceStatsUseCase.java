package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.FinanceStats;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.YearMonth;

public interface GetFinanceStatsUseCase {
    FinanceStats getStats(YearMonth month, SpaceMembership caller);
}
