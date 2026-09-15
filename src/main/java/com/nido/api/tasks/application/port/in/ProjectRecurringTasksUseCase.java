package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.ProjectedTaskOccurrence;

import java.time.LocalDate;
import java.util.List;

public interface ProjectRecurringTasksUseCase {
    List<ProjectedTaskOccurrence> project(SpaceMembership caller, LocalDate from, LocalDate to);
}
