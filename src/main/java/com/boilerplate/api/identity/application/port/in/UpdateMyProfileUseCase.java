package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.UpdateProfileCommand;

public interface UpdateMyProfileUseCase {
    void updateProfile(UpdateProfileCommand command);
}