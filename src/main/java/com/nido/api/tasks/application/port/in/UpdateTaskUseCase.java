package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;

public interface UpdateTaskUseCase {
    Task update(UpdateTaskCommand command, SpaceMembership caller);
}
