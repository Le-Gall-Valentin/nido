package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.application.dto.LoginCommand;
import com.nido.api.authentication.domain.model.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand command);
}