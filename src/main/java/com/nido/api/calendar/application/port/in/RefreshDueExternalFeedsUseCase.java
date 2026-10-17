package com.nido.api.calendar.application.port.in;


public interface RefreshDueExternalFeedsUseCase {
    /** Reads again every followed calendar, of every space, not asked for in six hours or so. */
    void refreshDue();
}
