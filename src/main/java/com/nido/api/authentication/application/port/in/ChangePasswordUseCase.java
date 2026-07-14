package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.application.dto.ChangePasswordResult;

import java.util.UUID;

public interface ChangePasswordUseCase {
    ChangePasswordResult changePassword(UUID userId, String currentPassword, String newPassword);
}