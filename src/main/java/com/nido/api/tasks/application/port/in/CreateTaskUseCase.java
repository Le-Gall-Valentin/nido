package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.Task;

public interface CreateTaskUseCase {
    Task create(CreateTaskCommand command, SpaceMembership caller);
}
