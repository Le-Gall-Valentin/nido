package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.ProfileUpdate;
import com.nido.api.identity.domain.model.UpdateProfileCommand;

public interface UpdateMyProfileUseCase {
    ProfileUpdate updateProfile(UpdateProfileCommand command);
}