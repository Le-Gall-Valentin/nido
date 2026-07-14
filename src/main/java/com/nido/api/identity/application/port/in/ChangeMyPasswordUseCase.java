package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.ChangeMyPasswordCommand;

public interface ChangeMyPasswordUseCase {
    void changeMyPassword(ChangeMyPasswordCommand command);
}