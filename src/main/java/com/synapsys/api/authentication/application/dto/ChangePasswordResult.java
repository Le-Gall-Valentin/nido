package com.synapsys.api.authentication.application.dto;

public sealed interface ChangePasswordResult
    permits ChangePasswordResult.Success,
            ChangePasswordResult.InvalidCurrentPassword,
            ChangePasswordResult.DataIntegrityError {

    record Success() implements ChangePasswordResult {}
    record InvalidCurrentPassword() implements ChangePasswordResult {}
    record DataIntegrityError() implements ChangePasswordResult {}
}