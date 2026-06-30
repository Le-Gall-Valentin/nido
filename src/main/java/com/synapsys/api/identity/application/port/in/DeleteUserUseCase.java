package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.DeleteUserCommand;

public interface DeleteUserUseCase {
    void delete(DeleteUserCommand command);
}