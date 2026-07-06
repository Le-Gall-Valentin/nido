package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.DeleteUserCommand;

public interface DeleteUserUseCase {
    void delete(DeleteUserCommand command);
}