package com.synapsys.api.identity.domain.port.out;

import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import com.synapsys.api.identity.domain.model.UpdateProfileCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.shared.model.Role;
import java.util.UUID;

public interface UserCommandPort {
    User createProfile(CreateUserProfileCommand command);
    void deactivate(UUID userId);
    void updateProfile(UpdateProfileCommand command);
    void updateRole(UUID userId, Role currentRole, Role newRole);
    void activate(UUID userId);
    void deleteGdpr(UUID userId);
}