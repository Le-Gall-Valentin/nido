package com.boilerplate.api.authentication.application.port.in;

import com.boilerplate.api.authentication.application.dto.LoginCommand;
import com.boilerplate.api.authentication.domain.model.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand command);
}