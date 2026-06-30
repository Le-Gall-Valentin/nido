package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.UpdateProfileCommand;

public interface UpdateMyProfileUseCase {
    void updateProfile(UpdateProfileCommand command);
}