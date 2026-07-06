package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.ChangeMyPasswordCommand;

public interface ChangeMyPasswordUseCase {
    void changeMyPassword(ChangeMyPasswordCommand command);
}