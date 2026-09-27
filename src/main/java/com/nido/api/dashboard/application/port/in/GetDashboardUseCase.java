package com.nido.api.dashboard.application.port.in;

import com.nido.api.dashboard.domain.model.Dashboard;
import com.nido.api.space.domain.model.SpaceMembership;

/** What deserves the caller's attention today in one space, already decided. */
public interface GetDashboardUseCase {
    Dashboard get(SpaceMembership caller, String callerEmail);
}
