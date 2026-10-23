package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.ImportReport;
import com.nido.api.space.domain.model.SpaceMembership;

public interface ImportCalendarUseCase {

    /** Reads the file as an import would, and writes nothing. */
    ImportReport analyse(String content, SpaceMembership caller);

    /** Reads the file again and writes it whole, or nothing. */
    ImportReport importFile(String content, SpaceMembership caller);
}
