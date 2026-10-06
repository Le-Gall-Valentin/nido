package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.CreateUserProfileCommand;
import com.nido.api.identity.domain.model.UpdateProfileCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;
import java.util.UUID;

public interface UserCommandPort {
    User createProfile(CreateUserProfileCommand command);
    /**
     * @return false when the account was already inactive — another administrator got there first, even after
     *         this one loaded it
     */
    boolean deactivate(UUID userId);
    void updateProfile(UpdateProfileCommand command);
    void updateRole(UUID userId, Role currentRole, Role newRole);
    /** @return false when the account was already active — see {@link #deactivate} */
    boolean activate(UUID userId);
    void deleteGdpr(UUID userId);
    void updateLanguage(UUID userId, Language language);
}