package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.UserSelfView;
import java.util.UUID;

public interface GetCurrentUserUseCase {
    UserSelfView getCurrentUser(UUID userId);
}