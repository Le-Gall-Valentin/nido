package com.synapsys.api.authentication.application.port.in;

import com.synapsys.api.authentication.application.dto.ChangePasswordResult;

import java.util.UUID;

public interface ChangePasswordUseCase {
    ChangePasswordResult changePassword(UUID userId, String currentPassword, String newPassword);
}