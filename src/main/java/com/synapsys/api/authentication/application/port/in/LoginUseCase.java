package com.synapsys.api.authentication.application.port.in;

import com.synapsys.api.authentication.application.dto.LoginCommand;
import com.synapsys.api.authentication.domain.model.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand command);
}