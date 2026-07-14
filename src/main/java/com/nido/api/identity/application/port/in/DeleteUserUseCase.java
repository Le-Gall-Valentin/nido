package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.DeleteUserCommand;

public interface DeleteUserUseCase {
    void delete(DeleteUserCommand command);
}