package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Projection;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.YearMonth;

public interface GetProjectionUseCase {
    Projection getProjection(YearMonth month, SpaceMembership caller);
}
