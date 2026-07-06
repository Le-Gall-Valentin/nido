package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.UserSelfView;
import java.util.UUID;

public interface GetCurrentUserUseCase {
    UserSelfView getCurrentUser(UUID userId);
}