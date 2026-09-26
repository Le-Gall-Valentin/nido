package com.nido.api.dashboard.domain.port.out;

import com.nido.api.space.domain.model.SpaceMembership;

/**
 * Brings a module's data up to date before any source reads it — today, materializing the recurring
 * occurrences that have fallen due. Every implementation lives in {@code infrastructure/source}.
 */
public interface DashboardPreparation {
    void prepare(SpaceMembership caller);
}
