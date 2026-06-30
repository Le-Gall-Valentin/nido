package com.synapsys.api.identity.domain.port.out;

import java.util.UUID;

public interface CredentialChangePort {
    void changePassword(UUID userId, String currentPassword, String newPassword);
}