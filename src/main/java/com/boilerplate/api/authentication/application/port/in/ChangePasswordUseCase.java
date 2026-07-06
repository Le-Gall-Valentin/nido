package com.boilerplate.api.authentication.application.port.in;

import com.boilerplate.api.authentication.application.dto.ChangePasswordResult;

import java.util.UUID;

public interface ChangePasswordUseCase {
    ChangePasswordResult changePassword(UUID userId, String currentPassword, String newPassword);
}