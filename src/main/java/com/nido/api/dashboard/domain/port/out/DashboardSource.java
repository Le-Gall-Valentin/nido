package com.nido.api.dashboard.domain.port.out;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;

/**
 * One contributor to the dashboard.
 *
 * <p>Every implementation lives in {@code infrastructure/source} and calls only the inbound use cases
 * and domain types of the module it reads, so the dependency runs one way: the dashboard depends on the
 * modules and none of them learns it exists. A source only reads — the preparation phase has already
 * made every due recurring occurrence real.
 */
public interface DashboardSource {

    CardKind kind();

    SourceResult read(DashboardContext context);
}
