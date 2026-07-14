package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.UserSelfView;
import java.util.UUID;

public interface GetCurrentUserUseCase {
    UserSelfView getCurrentUser(UUID userId);
}