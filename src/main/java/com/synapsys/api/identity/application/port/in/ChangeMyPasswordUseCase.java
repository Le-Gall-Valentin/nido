package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.ChangeMyPasswordCommand;

public interface ChangeMyPasswordUseCase {
    void changeMyPassword(ChangeMyPasswordCommand command);
}